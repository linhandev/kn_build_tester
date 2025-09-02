package com.knbuildtester.composeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knbuildtester.shared.facade.KnBuildTesterFacade
import kotlinx.coroutines.launch

@Composable
fun App() {
    var isInitialized by remember { mutableStateOf(false) }
    var performanceMetrics by remember { mutableStateOf<String?>(null) }
    var stressTestResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val facade = remember { KnBuildTesterFacade() }
    val scope = rememberCoroutineScope()
    
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KN Build Tester",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            if (!isInitialized) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = facade.initialize()
                                if (result.success) {
                                    isInitialized = true
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Text("Initializing...")
                    } else {
                        Text("Initialize Application")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Performance Metrics",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                val metrics = facade.getPerformanceMetrics()
                                                performanceMetrics = """
                                                    Total Entities: ${metrics.totalEntities}
                                                    Active Entities: ${metrics.activeEntities}
                                                    Average Score: ${"%.2f".format(metrics.averageScore)}
                                                    Memory Usage: ${metrics.memoryUsage / 1024 / 1024} MB
                                                    Processing Time: ${metrics.processingTime} ms
                                                """.trimIndent()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    Text("Get Metrics")
                                }
                                
                                performanceMetrics?.let { metrics ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = metrics,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Stress Testing",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val result = facade.executeStressTest(100)
                                                    stressTestResult = """
                                                        Entities: ${result.entityCount}
                                                        Success: ${result.successCount}
                                                        Failures: ${result.failureCount}
                                                        Duration: ${result.totalDuration} ms
                                                        Avg Time: ${"%.2f".format(result.averageProcessingTime)} ms
                                                        Throughput: ${"%.2f".format(result.throughput)} entities/sec
                                                    """.trimIndent()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Text("Run Small Test (100)")
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val result = facade.executeStressTest(1000)
                                                    stressTestResult = """
                                                        Entities: ${result.entityCount}
                                                        Success: ${result.successCount}
                                                        Failures: ${result.failureCount}
                                                        Duration: ${result.totalDuration} ms
                                                        Avg Time: ${"%.2f".format(result.averageProcessingTime)} ms
                                                        Throughput: ${"%.2f".format(result.throughput)} entities/sec
                                                    """.trimIndent()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Text("Run Large Test (1000)")
                                    }
                                }
                                
                                stressTestResult?.let { result ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = result,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Algorithm Benchmark",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                facade.executeComplexAlgorithms()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    Text("Run Algorithm Benchmark")
                                }
                            }
                        }
                    }
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}