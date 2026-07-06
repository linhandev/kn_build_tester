group = "org.cpf.kotlin"

// 版本号统一在 gradle.properties 的 klibVersion，子模块通过 allprojects 继承。
allprojects {
    version = property("klibVersion") as String
}
