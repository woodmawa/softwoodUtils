package bench

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole


/**
 * https://blog.avenuecode.com/java-microbenchmarks-with-jmh-part-1
 * https://blog.avenuecode.com/java-microbenchmarks-with-jmh-part-2
 * //https://blog.avenuecode.com/java-microbenchmarks-with-jmh-part-3
 */
class GroovyBenchmark {

    //Invocation: the method is called once for each call to the benchmark method.
    //Iteration: the method is called once for each iteration of the benchmark.
    //Trial: the method is called once for each full run of the benchmark, that is, a full fork including all warmup and benchmark iterations.
    @Setup(Level.Trial)
    static public void setup() throws IOException {

    }

    @TearDown(Level.Trial)
    static public void teardown() throws IOException {

    }

    //each thread running the benchmark will create its own instance of the state object.
    @State(Scope.Thread)
    public static class LocalRandom {
        Random random = new Random(System.nanoTime());
    }

    // each thread group running the benchmark will create its own instance of the state object.

    //all threads running the benchmark share the same state object.
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param(["1_000_000", "10_000_000", "100_000_000"])
        public int listSize

        public List<Integer> testList

        @Setup(Level.Trial)
        public void setUp() {
            testList = new Random()
                    .ints()
                    .limit(listSize)
                    .boxed()
                    .collect(Collectors.toList());
        }

    }

    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1)
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void longAdder(Blackhole blackhole, BenchmarkState state) {
        //LongAdder adder = new LongAdder();
        state.testList.parallelStream().forEach(adder::add);
        blackhole.consume(adder.sum());
    }

}