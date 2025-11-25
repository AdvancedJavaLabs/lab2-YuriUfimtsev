package worker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import config.AppConfig;
import config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import model.ResultMessage;
import model.ShutdownMessage;
import model.TaskMessage;
import processor.TextProcessor;
import util.SerializationUtils;

@Slf4j
public class WorkerApp {
    public static void main(String[] args) throws Exception {
        var app = new WorkerApp();
        app.run();
    }

    private void run() throws Exception {
        ConnectionFactory factory = RabbitConfig.connectionFactory();

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(RabbitConfig.TASK_QUEUE, false, false, false, null);
        channel.queueDeclare(RabbitConfig.RESULT_QUEUE, false, false, false, null);

        channel.basicQos(1);

        log.info("[Worker] Started. Waiting for messages...");

        var processor = new TextProcessor();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                Object message = SerializationUtils.deserialize(delivery.getBody());
                // Обрабатываем сообщение на shutdown
                if (message instanceof ShutdownMessage) {
                    log.info("[Worker] Received shutdown message");

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    channel.basicCancel(consumerTag);
                    channel.close();
                    connection.close();

                    log.info("[Worker] Stopped gracefully");
                    return;
                }

                TaskMessage task = (TaskMessage) message;
                log.info("[Worker] Received section {} of task {}", task.sectionId(), task.taskId());

                ResultMessage result = processor.process(task, AppConfig.TOP_N, AppConfig.NAME_REPLACEMENT);

                byte[] resultBody = SerializationUtils.serialize(result);
                channel.basicPublish("", RabbitConfig.RESULT_QUEUE, null, resultBody);
                log.info("[Worker] Sent result for section {}", task.sectionId());

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("[Worker] Unexpected exception", e);
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        channel.basicConsume(
                RabbitConfig.TASK_QUEUE,
                false,
                deliverCallback,
                consumerTag -> {}
        );
    }
}
