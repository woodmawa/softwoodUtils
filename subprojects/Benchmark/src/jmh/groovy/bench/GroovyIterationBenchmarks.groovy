package bench

import groovy.transform.CompileStatic
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

@State(Scope.Benchmark)
@CompileStatic
class GroovyIterationBenchmarks {

    final List<Integer> numbers = 0..1_000_000

    @Benchmark
    AtomicLong eachTest() {
        final AtomicLong result = new AtomicLong()
        numbers.each { result.addAndGet(it) }
        return result
    }

    @Benchmark
    AtomicLong forEachTest() {
        final AtomicLong result = new AtomicLong()
        for (int number : numbers) {
            result.addAndGet(number)
        }
        return result
    }

    @Benchmark
    AtomicLong forLoopTest() {
        final AtomicLong result = new AtomicLong()
        for (int i = 0; i < numbers.size(); i++) {
            result.addAndGet(numbers.get(i))
        }
        return result
    }

    @Benchmark
    AtomicLong iteratorTest() {
        final AtomicLong result = new AtomicLong()
        final Iterator<Integer> iterator = numbers.iterator()
        while (iterator.hasNext()) {
            result.addAndGet(iterator.next())
        }
        return result
    }

    @Benchmark
    AtomicLong java8ForEachWithClosureTest() {
        final AtomicLong result = new AtomicLong()
        numbers.forEach { result.addAndGet((int) it) }
        return result
    }

    @Benchmark
    AtomicLong java8ForEachWithAnonymousClassTest() {
        final AtomicLong result = new AtomicLong()
        numbers.forEach(new Consumer<Integer>() {
            @Override
            void accept(Integer number) {
                result.addAndGet(number)
            }
        })
        return result
    }
}