import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

// 保存对象引用，防止被GC回收
private val memoryHolders = mutableListOf<ByteArray>()

// 分配指定大小的内存并保持引用
private fun allocateMemory(sizeInMB: Int): ByteArray {
    val sizeInBytes = sizeInMB * 1024 * 1024
    val byteArray = ByteArray(sizeInBytes)
    byteArray.fill(1)
    return byteArray
}

@OptIn(NativeRuntimeApi::class)
fun main() {
    println("内存分配测试开始")
    
    val allocSizePerIterMB = 20 // 每次固定分配20MB
    var totalAllocatedMB = 0
    var iteration = 0
    
    try {
        while (true) {
            iteration++
            
            // 分配内存并保持引用
            val memory = allocateMemory(allocSizePerIterMB)
            memoryHolders.add(memory)
            
            totalAllocatedMB += allocSizePerIterMB
            
            // 每10次迭代输出一次内存使用情况
            if (iteration % 10 == 0) {
                println("已分配内存: $totalAllocatedMB MB, 迭代次数: $iteration")
            }
            
            // 短暂休眠，避免CPU使用率过高
            for (i in 0 until 5000000) {
                // 空循环作为简单的延迟
            }
            
            // 尝试触发GC，但不会回收我们持有引用的对象
            GC.collect()
        }
    } catch (e: Throwable) {
        println("程序异常: ${e.message}")
    } finally {
        println("程序结束，总共分配内存: $totalAllocatedMB MB")
    }
}

