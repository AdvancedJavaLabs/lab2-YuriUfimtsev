package benchmark;

import aggregator.AggregatorApp;
import producer.ProducerApp;
import worker.WorkerApp;

public class AppLauncher {
    public static Runnable producer(String inputFileName) {
        return () -> {
            try {
                ProducerApp.main(new String[]{"-i", inputFileName});
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public static Runnable aggregator(String reportFileName) {
        return () -> {
            try {
                AggregatorApp.main(new String[]{"-o", reportFileName});
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public static Runnable worker() {
        return () -> {
            try {
                WorkerApp.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
