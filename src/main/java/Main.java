import benchmark.BenchmarkRunner;

import java.io.FileWriter;

public class Main {
    public static void main(String[] args) throws Exception {
        try (FileWriter fileWriter = new FileWriter("benchmark_results.csv", false)) {
            fileWriter.write("input,workers,time_ms\n");
        }

        String[] inputs = {
                "10MB.txt",
                "50MB.txt",
                "100MB.txt"
        };

        int[] workerCounts = {1, 2, 4, 8};

        for (var file : inputs) {
            for (int workerCount : workerCounts) {
                BenchmarkRunner.runBenchmark(file, workerCount, "reports/benchmark_results.csv");
            }
        }
    }
}
