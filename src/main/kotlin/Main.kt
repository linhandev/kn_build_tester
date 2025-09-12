import kotlinx.coroutines.*
import java.util.concurrent.ForkJoinPool
import kotlin.system.measureTimeMillis

/**
 * Kotlin Multi-Processing Demo
 * Demonstrates various approaches to utilize multiple CPU cores
 */

fun main() {
    val parallelism = 4
    println("Parallelism: $parallelism")

    // Demo 1: Coroutines with CPU-intensive task
    println("Demo 1: Coroutines for CPU-intensive tasks")
    coroutinesDemo(parallelism)

//    // Demo 2: Parallel streams
//    println("Demo 2: Parallel streams")
//    parallelStreamsDemo()
//
//    println("\n" + "=" * 50)

    // Demo 3: Custom thread pool with concurrent processing
    println("Demo 3: Custom ForkJoinPool")
    customThreadPoolDemo(parallelism)

//    println("\n" + "=" * 50)
//
//    // Demo 4: Prime number calculation comparison
//    println("Demo 4: Prime number calculation - Sequential vs Parallel")
//    primeNumberDemo()
}

/**
 * Demo 1: Using Kotlin Coroutines for CPU-intensive tasks
 * This approach uses coroutines with a custom dispatcher for CPU-bound work
 */
fun coroutinesDemo(cores: Int) {
    val workSize = 10000000
    val chunkSize = workSize / cores
    
    // CPU-intensive dispatcher with number of threads = number of cores
    val cpuIntensiveDispatcher = Dispatchers.Default.limitedParallelism(cores)
    
    val sequentialTime = measureTimeMillis {
        val result = (1..workSize).map { heavyComputation(it) }.sum()
        println("Sequential result: $result")
    }
    
    val parallelTime = measureTimeMillis {
        runBlocking {
            val result = (0 until cores).map { coreIndex ->
                async(cpuIntensiveDispatcher) {
                    val start = coreIndex * chunkSize + 1
                    val end = if (coreIndex == cores - 1) workSize else (coreIndex + 1) * chunkSize
                    (start..end).map { heavyComputation(it) }.sum()
                }
            }.awaitAll().sum()
            println("Parallel result: $result")
        }
    }
    
    println("Sequential time: ${sequentialTime}ms")
    println("Parallel time: ${parallelTime}ms")
    println("Speedup: ${sequentialTime.toDouble() / parallelTime}x")
}

/**
 * Demo 2: Using Java's parallel streams (works well with Kotlin)
 */
fun parallelStreamsDemo() {
    val data = (1..1000000).toList()
    
    val sequentialTime = measureTimeMillis {
        val result = data.stream()
            .map { heavyComputation(it) }
            .reduce(0) { a, b -> a + b }
        println("Sequential streams result: $result")
    }
    
    val parallelTime = measureTimeMillis {
        val customPool = ForkJoinPool(4) // Limit to 4 cores
        val result = customPool.submit<Int> {
            data.parallelStream()
                .map { heavyComputation(it) }
                .reduce(0) { a, b -> a + b }
        }.get()
        customPool.shutdown()
        println("Parallel streams result: $result")
    }
    
    println("Sequential streams time: ${sequentialTime}ms")
    println("Parallel streams time: ${parallelTime}ms")
    println("Speedup: ${sequentialTime.toDouble() / parallelTime}x")
}

/**
 * Demo 3: Using custom ForkJoinPool to control parallelism
 */
fun customThreadPoolDemo(cores: Int) {
    val data = (1..10000000).toList()
    
    val customPool = ForkJoinPool(cores)
    
    val sequentialTime = measureTimeMillis {
        val result = data.map { heavyComputation(it) }.sum()
        println("Sequential custom pool result: $result")
    }
    
    val parallelTime = measureTimeMillis {
        val result = customPool.submit<Int> {
            data.parallelStream()
                .map { heavyComputation(it) }
                .reduce(0) { a, b -> a + b }
        }.get()
        println("Parallel custom pool result: $result")
    }
    
    customPool.shutdown()
    
    println("Sequential custom pool time: ${sequentialTime}ms")
    println("Parallel custom pool time: ${parallelTime}ms")
    println("Speedup: ${sequentialTime.toDouble() / parallelTime}x")
}

/**
 * Demo 4: Prime number calculation - comparing sequential vs parallel approaches
 */
fun primeNumberDemo() {
    val upperLimit = 100000
    
    val sequentialTime = measureTimeMillis {
        val primes = findPrimesSequential(upperLimit)
        println("Sequential primes found: ${primes.size}")
    }
    
    val parallelTime = measureTimeMillis {
        val primes = findPrimesParallel(upperLimit)
        println("Parallel primes found: ${primes.size}")
    }
    
    println("Sequential prime calculation time: ${sequentialTime}ms")
    println("Parallel prime calculation time: ${parallelTime}ms")
    println("Speedup: ${sequentialTime.toDouble() / parallelTime}x")
}

/**
 * CPU-intensive computation to simulate heavy work
 */
fun heavyComputation(n: Int): Int {
    var result = 0
    for (i in 1..100000) {
        result += (n * i) % 7
    }
    return result
}

/**
 * Sequential prime number finder
 */
fun findPrimesSequential(limit: Int): List<Int> {
    return (2..limit).filter { isPrime(it) }
}

/**
 * Parallel prime number finder using coroutines
 */
fun findPrimesParallel(limit: Int): List<Int> = runBlocking {
    val cores = Runtime.getRuntime().availableProcessors()
    val chunkSize = limit / cores
    val cpuDispatcher = Dispatchers.Default.limitedParallelism(cores)
    
    (0 until cores).map { coreIndex ->
        async(cpuDispatcher) {
            val start = coreIndex * chunkSize + if (coreIndex == 0) 2 else coreIndex * chunkSize + 1
            val end = if (coreIndex == cores - 1) limit else (coreIndex + 1) * chunkSize
            (start..end).filter { isPrime(it) }
        }
    }.awaitAll().flatten()
}

/**
 * Prime number checker
 */
fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    if (n == 2) return true
    if (n % 2 == 0) return false
    
    val limit = kotlin.math.sqrt(n.toDouble()).toInt()
    for (i in 3..limit step 2) {
        if (n % i == 0) return false
    }
    return true
}
