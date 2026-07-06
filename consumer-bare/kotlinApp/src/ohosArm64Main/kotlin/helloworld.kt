@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

// 隔离验证:本文件只调用 biz-klib 的 bizAip(),它引用 platform.DataAugmentationKit.AIP.AIP_OK
// (AIP 是 CPF 独有、HUAWEI 无的 platform 库)。不调任何其它 platform 符号,以隔离变量。
//
// 验证命题:
//  - 消费者加 implementation("com.example:ohos-capi") 独立 binding → bizAip() 能 link(独立 binding
//    提供了 platform.DataAugmentationKit.AIP package,按 FQN 找到 AIP_OK)
//  - 消费者不加独立 binding → bizAip() 符号找不到,link 失败
//  - biz-klib manifest depends 写 org.jetbrains.kotlin.native.platform.AIP(HUAWEI 无),不 patch,
//    对 link 无影响(unique_name 不被严格要求)

@CName("kn_helloworld")
fun helloworld(): String {
    // 调 bizAvTranscoder(),它引用 platform.MediaKit.AVTranscoder.OH_AVTranscoderConfig_Create(真函数)。
    // link 成功 ⇔ 消费者依赖里有提供 platform.MediaKit.AVTranscoder package 的库(独立 binding)。
    val ptr = bizAvTranscoder()
    return "bizAvTranscoder()=0x${ptr.toString(16)}"
}
