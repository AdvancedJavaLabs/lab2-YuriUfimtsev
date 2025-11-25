package aggregator;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import config.AppConfig;
import config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import model.FinalReport;
import model.ResultMessage;
import util.JsonUtils;
import util.SerializationUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AggregatorApp {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !args[0].equals("-o")) {
            throw new IllegalArgumentException("Usage: AggregatorApp -o <reportFileName>");
        }

        var outputPath = AppConfig.REPORTS_DIR + args[1];
        log.info("[Aggregator] Output file: {}", outputPath);
        var app = new AggregatorApp();
        app.run(outputPath);
    }

    private final CountDownLatch done = new CountDownLatch(1);
    private int totalSections;
    private String taskId;
    private final Map<Integer, ResultMessage> results = new HashMap<>();

    private void run(String reportFilePath) throws Exception {
        log.info("[Aggregator] Starting...");

        ConnectionFactory factory = RabbitConfig.connectionFactory();
        try (Connection connection = factory.newConnection();
             final var channel = connection.createChannel()) {

            channel.queueDeclare(RabbitConfig.RESULT_QUEUE, false, false, false, null);

            log.info("[Aggregator] Waiting for results...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    ResultMessage result = SerializationUtils.deserialize(delivery.getBody());
                    processResult(result, reportFilePath);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                } catch (Exception e) {
                    log.error("[Aggregator] Exception when processing result messages: ", e);
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };

            channel.basicConsume(
                    RabbitConfig.RESULT_QUEUE,
                    false,
                    deliverCallback,
                    consumerTag -> {}
            );

            done.await();
            log.info("[Aggregator] Finished gracefully");
        }
    }

    private void processResult(ResultMessage resultMessage, String reportFilePath) throws IOException {
        if (taskId == null) {
            taskId = resultMessage.taskId();
            totalSections = resultMessage.totalSections();
            log.info("[Aggregator] Starting new task {}", taskId);
        }

        results.put(resultMessage.sectionId(), resultMessage);
        log.info("[Aggregator] Got section {}", resultMessage.sectionId());

        if (results.size() == totalSections) {
            log.info("[Aggregator] Message reading is finished. Generating report...");
            buildFinalReport(reportFilePath);
        }
    }

    private void buildFinalReport(String reportFilePath) throws IOException {
        int wordCount = 0;
        int sentimentScore = 0;
        Map<String, Integer> globalFrequencies = new HashMap<>();
        List<String> allSentences = new ArrayList<>();
        StringBuilder replaced = new StringBuilder();

        for (int i = 0; i < totalSections; i++) {
            ResultMessage resultMessage  = results.get(i);

            wordCount += resultMessage.wordCount();
            sentimentScore += resultMessage.sentiment();

            resultMessage.topN().forEach((w, c) ->
                    globalFrequencies.merge(w, c, Integer::sum));

            replaced.append(resultMessage.replacedText()).append(" ");
            allSentences.addAll(resultMessage.sortedSentences());
        }

        Map<String, Integer> globalTop = globalFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(AppConfig.TOP_N)
                .collect(
                        LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll
                );

        allSentences.sort(Comparator.comparingInt(String::length));

        var report = new FinalReport(
                taskId,
                wordCount,
                sentimentScore,
                globalTop,
                replaced.toString().trim(),
                allSentences
        );

        JsonUtils.toJsonFile(report, reportFilePath);
        done.countDown();

        log.info("[Aggregator] Report is ready: saved in {}", reportFilePath);
    }
}
