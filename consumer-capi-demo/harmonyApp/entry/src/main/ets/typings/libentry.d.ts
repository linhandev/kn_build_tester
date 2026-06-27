/** libentry.so 原生扩展模块声明 */
declare module 'libentry.so' {
  /**
   * 执行 RDB（分布式数据管理 · 关系型数据库）CAPI 验证，返回多行中文报告（含返回值释义）。
   * 同步调用，与 ArkTS 触发线程一致，便于与 RDB 运行时 TLS 对齐。
   */
  export function runRdbModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /**
   * 执行 OH_CommonEvent（公共事件）CAPI 验证，返回多行中文报告。
   */
  export function runCommonEventModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /**
   * 执行 HuksKeyApi（HUKS 密钥管理）CAPI 验证，返回多行中文报告。
   */
  export function runHuksKeyModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /**
   * 模块7：Drawing（native_drawing）。fontTtfPath 为设备上 TTF 绝对路径；空串则跳过字体注册场景。
   */
  export function runDrawingModuleSmokeTest(fontTtfPath: string): string;
  /** NetConnection（网络连接）CAPI 验证报告。 */
  export function runNetConnectionModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** HiAppEvent（应用事件打点）CAPI 验证报告。 */
  export function runHiAppEventModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** HiLog（日志）CAPI 验证报告。 */
  export function runHiLogModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 模块10：ApiGuard / 分 API 等级弱符号与版本拦截验证报告（全量：函数 + 常量）。 */
  export function runVersionGuardModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 模块10：仅函数验证。 */
  export function runVersionGuardFuncSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 模块10：仅常量验证。 */
  export function runVersionGuardConstSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 自封装 cinterop：User.HiLog.OH_LOG_Print 循环 N 次，返回耗时文本。 */
  export function runHiLogOhLogPrintBenchCinteropSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 系统库：PerformanceAnalysisKit.HiLog.OH_LOG_Print 循环 N 次，返回耗时文本。 */
  export function runHiLogOhLogPrintBenchPlatformKitSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;

  /** 失败场景模块：各模块挑 1 个函数，传入空值/边界值/异常值构造失败返回。 */
  export function runFailureModuleSmokeTest(
    databaseDir: string,
    bundleName: string,
    moduleName: string,
  ): string;
}
