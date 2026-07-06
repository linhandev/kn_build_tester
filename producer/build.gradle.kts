group = "org.cpf.kotlin"

// 版本号统一在 gradle.properties 的 klibVersion，子模块通过 allprojects 继承，
// 不再各自写 version =。implementation(...) 也引用 klibVersion，避免散落硬编码。
allprojects {
    version = property("klibVersion") as String
}
