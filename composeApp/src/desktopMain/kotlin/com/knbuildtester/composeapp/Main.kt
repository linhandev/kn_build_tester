package com.knbuildtester.composeapp

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    println("🚀 KN Build Tester - Application")
    println("=================================")
    
    try {
        runBlocking {
            val facade = com.knbuildtester.shared.facade.KnBuildTesterFacade()
            
            println("📊 Initializing application...")
            val initResult = facade.initialize()
            
            if (initResult.success) {
                println("✅ Initialization successful: ${initResult.message}")
                
                println("\n📈 Getting performance metrics...")
                val metrics = facade.getPerformanceMetrics()
                
                println("📋 Performance Results:")
                println("  Total Entities: ${metrics.totalEntities}")
                println("  Active Entities: ${metrics.activeEntities}")
                println("  Average Score: ${"%.2f".format(metrics.averageScore)}")
                println("  Processing Time: ${metrics.processingTime} ms")
                
                println("\n⚡ Running stress test...")
                val stressResult = facade.executeStressTest(25)
                
                println("🧪 Stress Test Results:")
                println("  Entities Processed: ${stressResult.entityCount}")
                println("  Successful: ${stressResult.successCount}")
                println("  Failed: ${stressResult.failureCount}")
                println("  Total Duration: ${stressResult.totalDuration} ms")
                println("  Throughput: ${"%.2f".format(stressResult.throughput)} entities/sec")
                
                println("\n🎯 Application completed successfully!")
                
            } else {
                println("❌ Initialization failed: ${initResult.message}")
            }
        }
    } catch (e: Exception) {
        println("💥 Error: ${e.message}")
        e.printStackTrace()
    }
}