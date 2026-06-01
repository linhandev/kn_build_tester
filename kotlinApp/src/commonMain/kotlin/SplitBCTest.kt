package splitbc.test

import kotlin.math.*

// Module 1: Math utilities
object MathUtils {
    fun fibonacci(n: Int): Long {
        if (n <= 1) return n.toLong()
        var a = 0L; var b = 1L
        for (i in 2..n) { val t = a + b; a = b; b = t }
        return b
    }

    fun factorial(n: Int): Long {
        var r = 1L; for (i in 1..n) r *= i; return r
    }

    fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        if (n < 4) return true
        if (n % 2 == 0 || n % 3 == 0) return false
        var i = 5
        while (i * i <= n) { if (n % i == 0 || n % (i + 2) == 0) return false; i += 6 }
        return true
    }

    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
}

// Module 2: Matrix operations
class Matrix(val rows: Int, val cols: Int, val data: Array<DoubleArray>) {
    companion object {
        fun identity(size: Int) = Matrix(size, size, Array(size) { i -> DoubleArray(size) { j -> if (i == j) 1.0 else 0.0 } })
        fun random(rows: Int, cols: Int) = Matrix(rows, cols, Array(rows) { DoubleArray(cols) { kotlin.random.Random.nextDouble() } })
    }

    operator fun plus(other: Matrix): Matrix {
        require(rows == other.rows && cols == other.cols)
        return Matrix(rows, cols, Array(rows) { i -> DoubleArray(cols) { j -> data[i][j] + other.data[i][j] } })
    }

    operator fun times(other: Matrix): Matrix {
        require(cols == other.rows)
        val result = Matrix(rows, other.cols, Array(rows) { DoubleArray(other.cols) })
        for (i in 0 until rows)
            for (j in 0 until other.cols) {
                var sum = 0.0
                for (k in 0 until cols) sum += data[i][k] * other.data[k][j]
                result.data[i][j] = sum
            }
        return result
    }

    fun transpose(): Matrix = Matrix(cols, rows, Array(cols) { j -> DoubleArray(rows) { i -> data[i][j] } })

    fun trace(): Double {
        require(rows == cols)
        var sum = 0.0; for (i in 0 until rows) sum += data[i][i]; return sum
    }
}

// Module 3: Data structures
class BinaryTree<T : Comparable<T>> {
    private var root: Node<T>? = null

    private class Node<T>(val value: T, var left: Node<T>? = null, var right: Node<T>? = null)

    fun insert(value: T) { root = insertRec(root, value) }
    private fun insertRec(node: Node<T>?, value: T): Node<T> {
        if (node == null) return Node(value)
        when {
            value < node.value -> node.left = insertRec(node.left, value)
            value > node.value -> node.right = insertRec(node.right, value)
        }
        return node
    }

    fun inOrder(): List<T> {
        val result = mutableListOf<T>()
        fun traverse(node: Node<T>?) {
            if (node == null) return
            traverse(node.left); result.add(node.value); traverse(node.right)
        }
        traverse(root)
        return result
    }

    fun height(): Int = heightRec(root)
    private fun heightRec(node: Node<T>?): Int {
        if (node == null) return 0
        return 1 + maxOf(heightRec(node.left), heightRec(node.right))
    }
}

// Module 4: String processing
object StringUtils {
    fun compress(s: String): String {
        if (s.isEmpty()) return ""
        val sb = StringBuilder()
        var count = 1
        for (i in 1..s.length) {
            if (i < s.length && s[i] == s[i - 1]) count++
            else { sb.append(s[i - 1]); if (count > 1) sb.append(count); count = 1 }
        }
        return sb.toString()
    }

    fun longestPalindrome(s: String): String {
        if (s.length < 2) return s
        var start = 0; var maxLen = 1
        fun expand(left: Int, right: Int) {
            var l = left; var r = right
            while (l >= 0 && r < s.length && s[l] == s[r]) { l--; r++ }
            val len = r - l - 1
            if (len > maxLen) { maxLen = len; start = l + 1 }
        }
        for (i in s.indices) { expand(i, i); expand(i, i + 1) }
        return s.substring(start, start + maxLen)
    }

    fun permutations(s: String): List<String> {
        if (s.length <= 1) return listOf(s)
        val result = mutableListOf<String>()
        for (i in s.indices) {
            val rest = s.substring(0, i) + s.substring(i + 1)
            for (perm in permutations(rest)) result.add(s[i] + perm)
        }
        return result
    }
}

// Module 5: Graph algorithms
class Graph(val vertices: Int) {
    private val adj = Array(vertices) { mutableListOf<Int>() }

    fun addEdge(u: Int, v: Int) { adj[u].add(v); adj[v].add(u) }

    fun bfs(start: Int): List<Int> {
        val visited = BooleanArray(vertices)
        val queue = ArrayDeque<Int>()
        val result = mutableListOf<Int>()
        visited[start] = true; queue.add(start)
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst(); result.add(v)
            for (n in adj[v]) if (!visited[n]) { visited[n] = true; queue.add(n) }
        }
        return result
    }

    fun dfs(start: Int): List<Int> {
        val visited = BooleanArray(vertices)
        val result = mutableListOf<Int>()
        fun dfsRec(v: Int) {
            visited[v] = true; result.add(v)
            for (n in adj[v]) if (!visited[n]) dfsRec(n)
        }
        dfsRec(start); return result
    }

    fun shortestPath(start: Int, end: Int): Int {
        val dist = IntArray(vertices) { Int.MAX_VALUE }
        val queue = ArrayDeque<Int>()
        dist[start] = 0; queue.add(start)
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v == end) return dist[end]
            for (n in adj[v]) if (dist[n] == Int.MAX_VALUE) { dist[n] = dist[v] + 1; queue.add(n) }
        }
        return -1
    }
}

// Module 6: Numerical computation
object Numerics {
    fun integrate(f: (Double) -> Double, a: Double, b: Double, n: Int = 1000): Double {
        val h = (b - a) / n
        var sum = (f(a) + f(b)) / 2.0
        for (i in 1 until n) sum += f(a + i * h)
        return sum * h
    }

    fun newton(f: (Double) -> Double, df: (Double) -> Double, x0: Double, tol: Double = 1e-10, maxIter: Int = 100): Double {
        var x = x0
        for (i in 0 until maxIter) {
            val fx = f(x); val dfx = df(x)
            if (abs(dfx) < 1e-15) break
            val xNew = x - fx / dfx
            if (abs(xNew - x) < tol) return xNew
            x = xNew
        }
        return x
    }

    fun matPow(m: Matrix, p: Int): Matrix {
        var result = Matrix.identity(m.rows)
        var base = m
        var power = p
        while (power > 0) {
            if (power % 2 == 1) result = result * base
            base = base * base
            power /= 2
        }
        return result
    }
}

// Module 7: Collection algorithms
object CollectionAlgos {
    fun <T : Comparable<T>> mergeSort(arr: List<T>): List<T> {
        if (arr.size <= 1) return arr
        val mid = arr.size / 2
        return merge(mergeSort(arr.subList(0, mid)), mergeSort(arr.subList(mid, arr.size)))
    }

    private fun <T : Comparable<T>> merge(left: List<T>, right: List<T>): List<T> {
        val result = mutableListOf<T>()
        var i = 0; var j = 0
        while (i < left.size && j < right.size) {
            if (left[i] <= right[j]) result.add(left[i++]) else result.add(right[j++])
        }
        while (i < left.size) result.add(left[i++])
        while (j < right.size) result.add(right[j++])
        return result
    }

    fun <T : Comparable<T>> quickSort(arr: MutableList<T>, lo: Int = 0, hi: Int = arr.size - 1) {
        if (lo < hi) {
            val pivot = arr[hi]; var i = lo
            for (j in lo until hi) if (arr[j] <= pivot) { val t = arr[i]; arr[i] = arr[j]; arr[j] = t; i++ }
            val t = arr[i]; arr[i] = arr[hi]; arr[hi] = t
            quickSort(arr, lo, i - 1); quickSort(arr, i + 1, hi)
        }
    }

    fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.isEmpty()) return emptyList()
        val first = items[0]; val rest = items.subList(1, items.size)
        val withFirst = combinations(rest, k - 1).map { listOf(first) + it }
        val withoutFirst = combinations(rest, k)
        return withFirst + withoutFirst
    }
}

// Entry point for the test

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@CName("splitbc_test_run")
fun runTest(): String {
    val results = mutableListOf<String>()

    // Math tests
    results.add("fib(30)=${MathUtils.fibonacci(30)}")
    results.add("fact(15)=${MathUtils.factorial(15)}")
    results.add("primes<50=${(2..50).filter { MathUtils.isPrime(it) }.size}")

    // Matrix tests
    val m = Matrix.random(8, 8)
    val mt = m.transpose()
    val product = m * mt
    results.add("trace=${product.trace()}")

    // Binary tree
    val tree = BinaryTree<Int>()
    listOf(50, 30, 70, 20, 40, 60, 80, 10, 25, 35, 45).forEach { tree.insert(it) }
    results.add("tree_height=${tree.height()}")
    results.add("tree_size=${tree.inOrder().size}")

    // String utils
    results.add("compress=${StringUtils.compress("aabbbcccc")}")
    results.add("palindrome=${StringUtils.longestPalindrome("babad")}")

    // Graph
    val g = Graph(6)
    listOf(0 to 1, 0 to 2, 1 to 3, 2 to 3, 3 to 4, 4 to 5).forEach { (u, v) -> g.addEdge(u, v) }
    results.add("bfs=${g.bfs(0)}")
    results.add("shortest=${g.shortestPath(0, 5)}")

    // Numerical
    val pi = Numerics.integrate({ x -> 4.0 / (1.0 + x * x) }, 0.0, 1.0)
    results.add("pi≈$pi")

    // Sorting
    val sorted = CollectionAlgos.mergeSort(listOf(38, 27, 43, 3, 9, 82, 10))
    results.add("sorted=$sorted")

    return results.joinToString("\n")
}
