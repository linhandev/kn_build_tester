@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import platform.DataAugmentationKit.AIP.AIP_OK
import platform.MediaKit.AVTranscoder.OH_AVTranscoderConfig_Create

/**
 * 业务 klib:引用 CPF 独有、HUAWEI dist 无的 platform 库,验证"unique_name 对不上、FQN 对得上"穿刺。
 *
 *  - AIP_OK:enum 常量(cinterop 内联进 IR,不引函数符号)——对照,不触发 depends 解析
 *  - OH_AVTranscoderConfig_Create():真函数符号(返回 OH_AVTranscoder_Config*)——必须从提供
 *    platform.MediaKit.AVTranscoder package 的库里解析符号。cpf 0.4 生产时解析到 dist 内置的
 *    org.jetbrains.kotlin.native.platform.AVTranscoder,本 klib manifest depends 会写这个坐标。
 *    HUAWEI 消费者 dist 没有这个子包(MediaKit 聚合里无 AVTranscoder 子包),独立封装库
 *    com.example:ohos-capi-cinterop-AVTranscoder 的 unique_name 对不上但 package FQN 一致。
 *    消费者不 patch depends、只依赖 ohos-capi 聚合,看 link 能否按 FQN 解析到函数符号。
 */
@CName("kn_biz_aip")
fun bizAip(): UInt = AIP_OK

@CName("kn_biz_avtranscoder")
fun bizAvTranscoder(): Long {
    // 调真函数:OH_AVTranscoderConfig_Create() 无参返回指针。
    // link 成功 ⇔ 消费者依赖里有提供 platform.MediaKit.AVTranscoder package 的库(独立封装)。
    val ptr = OH_AVTranscoderConfig_Create()!!
    return ptr.rawValue.toLong()
}
