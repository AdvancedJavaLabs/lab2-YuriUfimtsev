package benchmark;

import config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import model.ShutdownMessage;
import util.SerializationUtils;

import java.io.FileWriter;

@Slf4j
public class BenchmarkRunner {
    public static void runBenchmark(
            String inputFileName,
            int workersCount,
            String csvOutput
    ) throws Exception {
        log.info("=== Benchmark start ===");
        log.info("Input: {}", inputFileName);
        log.info("Workers: {}", workersCount);

        // Очищаем очереди после предыдущих прогонов
        try (var connection = RabbitConfig.connectionFactory().newConnection();
             var channel = connection.createChannel()) {
            channel.queueDeclare(RabbitConfig.TASK_QUEUE, false, false, false, null);
            channel.queuePurge(RabbitConfig.TASK_QUEUE);

            channel.queueDeclare(RabbitConfig.RESULT_QUEUE, false, false, false, null);
            channel.queuePurge(RabbitConfig.RESULT_QUEUE);
        }

        var reportName = String.format("report_%s_w%s.json", inputFileName, workersCount);
        var aggregatorThread = new Thread(AppLauncher.aggregator(reportName), "aggregator");
        aggregatorThread.start();

        var workerThreads = new Thread[workersCount];
        for (int i = 0; i < workersCount; ++i) {
            workerThreads[i] = new Thread(AppLauncher.worker(), "worker-" + i);
            workerThreads[i].start();
        }

        Thread producerThread = new Thread(AppLauncher.producer(inputFileName), "producer");

        long start = System.nanoTime();
        producerThread.start();

        producerThread.join();
        aggregatorThread.join();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Benchmark finished: {} ms", elapsedMs);

        // Завершаем worker-ов с помощью рассылки ShutdownMessage
        try (var connection = RabbitConfig.connectionFactory().newConnection();
             var channel = connection.createChannel()) {

            for (int i = 0; i < workersCount; i++) {
                var message = SerializationUtils.serialize(new ShutdownMessage());
                channel.basicPublish("", RabbitConfig.TASK_QUEUE, null, message);
            }
        }
        log.info("Sent shutdown messages to workers");

        try (FileWriter fileWriter = new FileWriter(csvOutput, true)) {
            fileWriter.write(String.join(",",
                    inputFileName,
                    reportName,
                    String.valueOf(workersCount),
                    String.valueOf(elapsedMs)
            ));
            fileWriter.write("\n");
        }

        log.info("CSV updated: {}", csvOutput);
        log.info("=== Benchmark end ===");
    }
}
