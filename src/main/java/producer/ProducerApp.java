package producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import config.AppConfig;
import config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import model.TaskMessage;
import util.SerializationUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ProducerApp {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !args[0].equals("-i")) {
            throw new IllegalArgumentException("Usage: ProducerApp -i <inputFileName>");
        }

        var inputPath = AppConfig.INPUT_DIR + args[1];
        if (!Files.exists(Path.of(inputPath))) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }

        log.info("[Producer] Using input file: {}", inputPath);
        var app = new ProducerApp();
        app.run(inputPath);
    }

    private void run(String inputFile) throws Exception {
        var fullText = Files.readString(Path.of(inputFile));
        var sections = splitIntoSections(fullText, AppConfig.SENTENCES_PER_SECTION);

        var taskId = UUID.randomUUID().toString();
        var totalSections = sections.size();

        log.info("[Producer] TaskId = {}, sections = {}", taskId, totalSections);

        var factory = RabbitConfig.connectionFactory();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(RabbitConfig.TASK_QUEUE, false, false, false, null);

            for (int i = 0; i < sections.size(); i++) {

                var taskMessage = new TaskMessage(
                        taskId,
                        i,
                        totalSections,
                        sections.get(i)
                );

                var serializedMessage = SerializationUtils.serialize(taskMessage);

                channel.basicPublish("", RabbitConfig.TASK_QUEUE, null, serializedMessage);
                log.info("[Producer] Sent section {}", i);
            }

            log.info("[Producer] All sections sent.");
        }

    }

    private List<String> splitIntoSections(String text, int sentencesPerSection) {
        List<String> sentences = Arrays.stream(text.split("[.!?]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int counter = 0;

        for (String sentence : sentences) {
            current.append(sentence).append(". ");
            counter++;

            if (counter == sentencesPerSection) {
                sections.add(current.toString().trim());
                current.setLength(0);
                counter = 0;
            }
        }

        if (!current.isEmpty()) {
            sections.add(current.toString().trim());
        }

        return sections;
    }
}
