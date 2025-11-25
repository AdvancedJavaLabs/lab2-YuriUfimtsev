package config;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitConfig {
    public static final String HOST = "localhost";

    public static final String TASK_QUEUE = "tasks";
    public static final String RESULT_QUEUE = "results";

    public static ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(HOST);
        factory.setUsername("guest");
        factory.setPassword("guest");

        return factory;
    }
}
