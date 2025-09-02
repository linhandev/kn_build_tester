package com.knbuildtester.composeapp

import com.knbuildtester.shared.facade.KnBuildTesterFacade
import kotlinx.coroutines.runBlocking

fun main() {
    println("KN Build Tester - Application")
    println("==============================")
    
    val facade = KnBuildTesterFacade()
    
    runBlocking {
        // Initialize the application
        println("Initializing application...")
        val initResult = facade.initialize()
        
        if (initResult.success) {
            println("✓ Initialization successful: ${initResult.message}")
            
            // Get performance metrics
            println("\nGetting performance metrics...")
            val metrics = facade.getPerformanceMetrics()
            println("Total Entities: ${metrics.totalEntities}")
            println("Active Entities: ${metrics.activeEntities}")
            println("Average Score: ${"%.2f".format(metrics.averageScore)}")
            println("Memory Usage: ${metrics.memoryUsage / 1024 / 1024} MB")
            println("Processing Time: ${metrics.processingTime} ms")
            
            // Run stress test
            println("\nRunning stress test...")
            val stressResult = facade.executeStressTest(50)
            println("Stress Test Results:")
            println("  Entities Processed: ${stressResult.entityCount}")
            println("  Successful: ${stressResult.successCount}")
            println("  Failed: ${stressResult.failureCount}")
            println("  Total Duration: ${stressResult.totalDuration} ms")
            println("  Average Processing Time: ${"%.2f".format(stressResult.averageProcessingTime)} ms")
            println("  Throughput: ${"%.2f".format(stressResult.throughput)} entities/sec")
            
        } else {
            println("✗ Initialization failed: ${initResult.message}")
        }
    }
    
    println("\nApplication completed.")
}