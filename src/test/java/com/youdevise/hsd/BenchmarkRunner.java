package com.youdevise.hsd;

import com.google.common.base.Strings;

public class BenchmarkRunner {
    private final int totalRuns;
    public BenchmarkRunner(int totalRuns) {
        this.totalRuns = totalRuns;
    }
    

    public void runBenchmark(final String description, Runnable benchmark) {
        long warmupTime = 0;
        long[] times = new long[totalRuns];
        long totalTime = 0;
        long fastestTime = 100000L;
        long slowestTime = 0;
        for (int i = 0; i<=totalRuns; i++) {
            long start = System.nanoTime();
            benchmark.run();
            long end = System.nanoTime();
            long time = (end - start) / 1000000;
            if (i == 0) {
                warmupTime = time;
            } else {
                totalTime += time;
                if (time < fastestTime) { fastestTime = time; }
                if (time > slowestTime) { slowestTime = time; }
                times[i-1] = time;
            }
        }
        double mean = totalTime / totalRuns;
        double numerator = 0;
        for (long time : times) {
            numerator += (time * time)  + (mean * mean) - (2 * mean * time);
        }
        double stdDev = Math.sqrt(numerator / totalRuns);
        System.out.println(description);
        System.out.println(Strings.repeat("=", description.length()));
        System.out.println(String.format("Avg over %d runs: %d ms", totalRuns, totalTime / totalRuns));
        System.out.println(String.format("Warmup: %d ms", warmupTime));
        System.out.println(String.format("First run: %d ms", times[0]));
        System.out.println(String.format("Slowest: %d ms", slowestTime));
        System.out.println(String.format("Fastest: %d ms", fastestTime));
        System.out.println(String.format("Standard deviation: %s", stdDev));
        System.out.println();
    }
}