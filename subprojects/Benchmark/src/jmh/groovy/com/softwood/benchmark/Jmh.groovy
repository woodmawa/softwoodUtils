package com.softwood.benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode

class Jmh {

    @Benchmark
    @BenchmarkMode(org.openjdk.jmh.annotations.Mode.AverageTime)
    static def benchmark (Closure work) {
        work()
    }
}
