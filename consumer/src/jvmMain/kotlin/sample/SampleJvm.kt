package sample

actual class Sample {
    actual fun checkMe() = 42
}

actual object Platform {
    actual val name: String = "JVM"
}

fun main() {
    val common = CommonClass()
    val existingParam = "existingParamJVMValue"
    common.publicMethod(existingParam)
    common.publicPrivateMethodCaller(existingParam)
}
