// Minimal reproduction for splitBCfile=2 compile crash on OHOS arm64
// konan cpf version 2.2.21-0.3.0-04

// Multiple classes and functions to generate enough bitcode for splitBCfile to activate

class DataProcessor(val name: String) {
    private val data = mutableListOf<Int>()

    fun addData(value: Int) {
        data.add(value)
    }

    fun processData(): List<Int> {
        return data.map { it * 2 }
            .filter { it > 10 }
            .sorted()
    }

    fun computeSum(): Int {
        return data.fold(0) { acc, v -> acc + v }
    }

    fun computeAverage(): Double {
        if (data.isEmpty()) return 0.0
        return computeSum().toDouble() / data.size
    }
}

class MathUtils {
    companion object {
        fun factorial(n: Int): Long {
            if (n <= 1) return 1
            var result = 1L
            for (i in 2..n) {
                result *= i
            }
            return result
        }

        fun fibonacci(n: Int): Long {
            if (n <= 1) return n.toLong()
            var a = 0L
            var b = 1L
            for (i in 2..n) {
                val temp = a + b
                a = b
                b = temp
            }
            return b
        }

        fun gcd(a: Int, b: Int): Int {
            var x = a
            var y = b
            while (y != 0) {
                val temp = y
                y = x % y
                x = temp
            }
            return x
        }

        fun lcm(a: Int, b: Int): Int {
            return a * b / gcd(a, b)
        }

        fun isPrime(n: Int): Boolean {
            if (n < 2) return false
            if (n < 4) return true
            if (n % 2 == 0 || n % 3 == 0) return false
            var i = 5
            while (i * i <= n) {
                if (n % i == 0 || n % (i + 2) == 0) return false
                i += 6
            }
            return true
        }
    }
}

sealed class Shape {
    abstract fun area(): Double
    abstract fun perimeter(): Double

    class Circle(val radius: Double) : Shape() {
        override fun area(): Double = kotlin.math.PI * radius * radius
        override fun perimeter(): Double = 2 * kotlin.math.PI * radius
    }

    class Rectangle(val width: Double, val height: Double) : Shape() {
        override fun area(): Double = width * height
        override fun perimeter(): Double = 2 * (width + height)
    }

    class Triangle(val a: Double, val b: Double, val c: Double) : Shape() {
        override fun area(): Double {
            val s = (a + b + c) / 2
            return kotlin.math.sqrt(s * (s - a) * (s - b) * (s - c))
        }
        override fun perimeter(): Double = a + b + c
    }
}

interface Sortable {
    fun sort(data: IntArray): IntArray
}

class BubbleSort : Sortable {
    override fun sort(data: IntArray): IntArray {
        val arr = data.copyOf()
        for (i in arr.indices) {
            for (j in 0 until arr.size - 1 - i) {
                if (arr[j] > arr[j + 1]) {
                    val temp = arr[j]
                    arr[j] = arr[j + 1]
                    arr[j + 1] = temp
                }
            }
        }
        return arr
    }
}

class QuickSort : Sortable {
    override fun sort(data: IntArray): IntArray {
        val arr = data.copyOf()
        quickSort(arr, 0, arr.size - 1)
        return arr
    }

    private fun quickSort(arr: IntArray, low: Int, high: Int) {
        if (low < high) {
            val pivot = partition(arr, low, high)
            quickSort(arr, low, pivot - 1)
            quickSort(arr, pivot + 1, high)
        }
    }

    private fun partition(arr: IntArray, low: Int, high: Int): Int {
        val pivot = arr[high]
        var i = low - 1
        for (j in low until high) {
            if (arr[j] <= pivot) {
                i++
                val temp = arr[i]
                arr[i] = arr[j]
                arr[j] = temp
            }
        }
        val temp = arr[i + 1]
        arr[i + 1] = arr[high]
        arr[high] = temp
        return i + 1
    }
}

class MergeSort : Sortable {
    override fun sort(data: IntArray): IntArray {
        val arr = data.copyOf()
        mergeSort(arr, 0, arr.size - 1)
        return arr
    }

    private fun mergeSort(arr: IntArray, left: Int, right: Int) {
        if (left < right) {
            val mid = (left + right) / 2
            mergeSort(arr, left, mid)
            mergeSort(arr, mid + 1, right)
            merge(arr, left, mid, right)
        }
    }

    private fun merge(arr: IntArray, left: Int, mid: Int, right: Int) {
        val n1 = mid - left + 1
        val n2 = right - mid
        val l = IntArray(n1)
        val r = IntArray(n2)
        for (i in 0 until n1) l[i] = arr[left + i]
        for (j in 0 until n2) r[j] = arr[mid + 1 + j]
        var i = 0; var j = 0; var k = left
        while (i < n1 && j < n2) {
            if (l[i] <= r[j]) { arr[k] = l[i]; i++ }
            else { arr[k] = r[j]; j++ }
            k++
        }
        while (i < n1) { arr[k] = l[i]; i++; k++ }
        while (j < n2) { arr[k] = r[j]; j++; k++ }
    }
}

enum class Color { RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA }

data class Pixel(val x: Int, val y: Int, val color: Color)

class Canvas(val width: Int, val height: Int) {
    private val pixels = Array(width * height) { Pixel(it % width, it / width, Color.RED) }

    fun setPixel(x: Int, y: Int, color: Color) {
        if (x in 0 until width && y in 0 until height) {
            pixels[y * width + x] = Pixel(x, y, color)
        }
    }

    fun getPixel(x: Int, y: Int): Pixel? {
        return if (x in 0 until width && y in 0 until height) pixels[y * width + x] else null
    }

    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                setPixel(x + dx, y + dy, color)
            }
        }
    }
}

fun main() {
    println("splitBCfile reproduction test")

    val processor = DataProcessor("test")
    for (i in 1..100) {
        processor.addData(i)
    }
    val processed = processor.processData()
    println("Processed ${processed.size} items, sum = ${processor.computeSum()}")

    println("Factorial(10) = ${MathUtils.factorial(10)}")
    println("Fibonacci(20) = ${MathUtils.fibonacci(20)}")
    println("GCD(48, 18) = ${MathUtils.gcd(48, 18)}")
    println("IsPrime(97) = ${MathUtils.isPrime(97)}")

    val shapes = listOf(
        Shape.Circle(5.0),
        Shape.Rectangle(3.0, 4.0),
        Shape.Triangle(3.0, 4.0, 5.0)
    )
    shapes.forEach { println("Area: ${it.area()}, Perimeter: ${it.perimeter()}") }

    val data = intArrayOf(64, 34, 25, 12, 22, 11, 90)
    val sorters = listOf(BubbleSort(), QuickSort(), MergeSort())
    sorters.forEach { sorter ->
        val sorted = sorter.sort(data)
        println("${sorter::class.simpleName}: ${sorted.toList()}")
    }

    val canvas = Canvas(100, 100)
    canvas.fillRect(10, 10, 20, 20, Color.BLUE)
    println("Pixel at (15,15): ${canvas.getPixel(15, 15)}")
}
