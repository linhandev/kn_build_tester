import com.example.switchlib.processValue
import com.example.switchlib.processValueCond
import com.example.sugarlib.complexSugarFunction

actual fun runGcovTest() {
    val size = 10
    val overflow = 1000
    if (size > overflow) {
        println("size wins")
    } else {
        println("overflow wins")
    }

    processValue()
    processValueCond()
    complexSugarFunction()
}
