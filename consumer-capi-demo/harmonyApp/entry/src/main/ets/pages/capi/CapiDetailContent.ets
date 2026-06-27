import * as lib from 'libentry.so';
import { common } from '@kit.AbilityKit';
import { fileIo } from '@kit.CoreFileKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import deviceInfo from '@ohos.deviceInfo';

const RDB_TAG = 'RdbCapi';
const RDB_DOMAIN = 0xF001;

const COMMON_EVENT_TAG = 'CommonEventCapi';
const COMMON_EVENT_DOMAIN = 0xF002;

const HUKS_KEY_TAG = 'HuksKeyCapi';
const HUKS_KEY_DOMAIN = 0xF003;

const DRAWING_TAG = 'DrawingCapi';
const DRAWING_DOMAIN = 0xF004;
const NET_CONN_TAG = 'NetConnectionCapi';
const NET_CONN_DOMAIN = 0xF004;

const HI_APP_EVENT_TAG = 'HiAppEventCapi';
const HI_APP_EVENT_DOMAIN = 0xF005;

const HI_LOG_CAP_TAG = 'HiLogCapi';
const HI_LOG_CAP_DOMAIN = 0xF006;

const VERSION_GUARD_TAG = 'VersionGuardCapi';
const VERSION_GUARD_DOMAIN = 0xF007;

/**
 * 模块详情内容（非独立页面，由 Index 内嵌展示，避免 router 全页转场引发崩溃）。
 */
@Component
export struct CapiDetailContent {
  @Prop moduleKey: string = '';
  @Prop filesDir: string = '';
  @Prop databaseDir: string = '';
  @Prop bundleName: string = '';
  @Prop moduleName: string = '';

  @State rdbReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State rdbBusy: boolean = false;

  @State commonEventReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State commonEventBusy: boolean = false;

  @State huksKeyReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State huksKeyBusy: boolean = false;

  @State drawingReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State drawingBusy: boolean = false;
  @State netConnectionReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State netConnectionBusy: boolean = false;

  @State hiAppEventReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State hiAppEventBusy: boolean = false;

  @State hiLogReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State hiLogBusy: boolean = false;

  @State failureReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State failureBusy: boolean = false;

  @State versionGuardDeviceBanner: string = '当前设备 SDK API：…';
  @State versionGuardReport: string = '点击「运行验证」查看各 API 返回值说明。';
  @State versionGuardBusy: boolean = false;
  @State versionGuardBenchCinteropReport: string = '点击「运行验证」：仅自封装 cinterop（User.HiLog.OH_LOG_Print）循环 N 次并显示耗时。';
  @State versionGuardBenchPlatformReport: string = '点击「运行验证」：仅系统库（PerfKit.HiLog.OH_LOG_Print）循环 N 次并显示耗时。';
  @State versionGuardBenchCinteropBusy: boolean = false;
  @State versionGuardBenchPlatformBusy: boolean = false;

  private runFailureFlow(): void {
    if (this.failureBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.failureReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.failureBusy = true;
    try {
      hilog.info(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG,
        'runFailure begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runFailureModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.failureReport = text ?? '(无返回内容)';
      hilog.info(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG, 'runFailure done len=%{public}d', this.failureReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.failureReport = `调用失败: ${msg}`;
      hilog.error(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG, 'runFailure err %{public}s', msg);
    } finally {
      this.failureBusy = false;
    }
  }

  private runRdbFlow(): void {
    if (this.rdbBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.rdbReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.rdbBusy = true;
    try {
      hilog.info(RDB_DOMAIN, RDB_TAG, 'runRdb begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      // 传 UIAbilityContext.databaseDir + Kotlin 内配对 EL2；空串时 Kotlin 仍回落 HiLog 的 el3 路径（部分应用无 el3 权限会 14800000）
      const text: string = lib.runRdbModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.rdbReport = text ?? '(无返回内容)';
      hilog.info(RDB_DOMAIN, RDB_TAG, 'runRdb done len=%{public}d', this.rdbReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.rdbReport = `调用失败: ${msg}`;
      hilog.error(RDB_DOMAIN, RDB_TAG, 'runRdb err %{public}s', msg);
    } finally {
      this.rdbBusy = false;
    }
  }

  private runCommonEventFlow(): void {
    if (this.commonEventBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.commonEventReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.commonEventBusy = true;
    try {
      hilog.info(COMMON_EVENT_DOMAIN, COMMON_EVENT_TAG,
        'runCommonEvent begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runCommonEventModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.commonEventReport = text ?? '(无返回内容)';
      hilog.info(COMMON_EVENT_DOMAIN, COMMON_EVENT_TAG, 'runCommonEvent done len=%{public}d', this.commonEventReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.commonEventReport = `调用失败: ${msg}`;
      hilog.error(COMMON_EVENT_DOMAIN, COMMON_EVENT_TAG, 'runCommonEvent err %{public}s', msg);
    } finally {
      this.commonEventBusy = false;
    }
  }

  private runHuksKeyFlow(): void {
    if (this.huksKeyBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.huksKeyReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.huksKeyBusy = true;
    try {
      hilog.info(HUKS_KEY_DOMAIN, HUKS_KEY_TAG,
        'runHuksKey begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runHuksKeyModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.huksKeyReport = text ?? '(无返回内容)';
      hilog.info(HUKS_KEY_DOMAIN, HUKS_KEY_TAG, 'runHuksKey done len=%{public}d', this.huksKeyReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.huksKeyReport = `调用失败: ${msg}`;
      hilog.error(HUKS_KEY_DOMAIN, HUKS_KEY_TAG, 'runHuksKey err %{public}s', msg);
    } finally {
      this.huksKeyBusy = false;
    }
  }

  private runDrawingFlow(): void {
    if (this.drawingBusy) {
      return;
    }
    this.drawingBusy = true;
    let fontPath = '';
    try {
      const ctx = getContext(this) as common.UIAbilityContext;
      const resMgr = ctx.resourceManager;
      const data = resMgr.getRawFileContentSync('font/Apple_Chancery.ttf');
      fontPath = `${this.filesDir}/Apple_Chancery_smoke.ttf`;
      const f = fileIo.openSync(
        fontPath,
        fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE | fileIo.OpenMode.TRUNC,
      );
      fileIo.writeSync(f.fd, data);
      fileIo.closeSync(f);
    } catch (e) {
      const msg = `${e}`;
      fontPath = '';
      hilog.warn(DRAWING_DOMAIN, DRAWING_TAG, 'copy Apple_Chancery.ttf to filesDir failed %{public}s', msg);
    }
    try {
      hilog.info(DRAWING_DOMAIN, DRAWING_TAG, 'runDrawing begin fontPath=%{public}s', fontPath);
      const text: string = lib.runDrawingModuleSmokeTest(fontPath);
      this.drawingReport = text ?? '(无返回内容)';
      hilog.info(DRAWING_DOMAIN, DRAWING_TAG, 'runDrawing done len=%{public}d', this.drawingReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.drawingReport = `调用失败: ${msg}`;
      hilog.error(DRAWING_DOMAIN, DRAWING_TAG, 'runDrawing err %{public}s', msg);
    } finally {
      this.drawingBusy = false;
    }
  }
  private runNetConnectionFlow(): void {
    if (this.netConnectionBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.netConnectionReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.netConnectionBusy = true;
    try {
      hilog.info(NET_CONN_DOMAIN, NET_CONN_TAG,
        'runNetConnection begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runNetConnectionModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.netConnectionReport = text ?? '(无返回内容)';
      hilog.info(NET_CONN_DOMAIN, NET_CONN_TAG, 'runNetConnection done len=%{public}d', this.netConnectionReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.netConnectionReport = `调用失败: ${msg}`;
      hilog.error(NET_CONN_DOMAIN, NET_CONN_TAG, 'runNetConnection err %{public}s', msg);
    } finally {
      this.netConnectionBusy = false;
    }
  }

  private runHiAppEventFlow(): void {
    if (this.hiAppEventBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.hiAppEventReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.hiAppEventBusy = true;
    try {
      hilog.info(HI_APP_EVENT_DOMAIN, HI_APP_EVENT_TAG,
        'runHiAppEvent begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runHiAppEventModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.hiAppEventReport = text ?? '(无返回内容)';
      hilog.info(HI_APP_EVENT_DOMAIN, HI_APP_EVENT_TAG, 'runHiAppEvent done len=%{public}d', this.hiAppEventReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.hiAppEventReport = `调用失败: ${msg}`;
      hilog.error(HI_APP_EVENT_DOMAIN, HI_APP_EVENT_TAG, 'runHiAppEvent err %{public}s', msg);
    } finally {
      this.hiAppEventBusy = false;
    }
  }

  private refreshVersionGuardDeviceBanner(): void {
    try {
      const api = deviceInfo.sdkApiVersion;
      this.versionGuardDeviceBanner = `当前设备 SDK API：${api}`;
    } catch (e) {
      const msg = `${e}`;
      this.versionGuardDeviceBanner = `当前设备 SDK API：读取失败 ${msg}`;
      hilog.error(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'deviceInfo.sdkApiVersion err %{public}s', msg);
    }
  }

  private runVersionGuardFlow(): void {
    if (this.versionGuardBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.versionGuardReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.versionGuardBusy = true;
    try {
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG,
        'runVersionGuard begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runVersionGuardModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.versionGuardReport = text ?? '(无返回内容)';
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runVersionGuard done len=%{public}d', this.versionGuardReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.versionGuardReport = `调用失败: ${msg}`;
      hilog.error(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runVersionGuard err %{public}s', msg);
    } finally {
      this.versionGuardBusy = false;
    }
  }

  private runVersionGuardHiLogBenchCinteropFlow(): void {
    if (this.versionGuardBenchCinteropBusy || this.versionGuardBenchPlatformBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.versionGuardBenchCinteropReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.versionGuardBenchCinteropBusy = true;
    try {
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG,
        'runHiLogBenchCinterop dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runHiLogOhLogPrintBenchCinteropSmokeTest(
        this.databaseDir, this.bundleName, this.moduleName);
      this.versionGuardBenchCinteropReport = text ?? '(无返回内容)';
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runHiLogBenchCinterop done len=%{public}d',
        this.versionGuardBenchCinteropReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.versionGuardBenchCinteropReport = `调用失败: ${msg}`;
      hilog.error(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runHiLogBenchCinterop err %{public}s', msg);
    } finally {
      this.versionGuardBenchCinteropBusy = false;
    }
  }

  private runVersionGuardHiLogBenchPlatformFlow(): void {
    if (this.versionGuardBenchCinteropBusy || this.versionGuardBenchPlatformBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.versionGuardBenchPlatformReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.versionGuardBenchPlatformBusy = true;
    try {
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG,
        'runHiLogBenchPlatform dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runHiLogOhLogPrintBenchPlatformKitSmokeTest(
        this.databaseDir, this.bundleName, this.moduleName);
      this.versionGuardBenchPlatformReport = text ?? '(无返回内容)';
      hilog.info(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runHiLogBenchPlatform done len=%{public}d',
        this.versionGuardBenchPlatformReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.versionGuardBenchPlatformReport = `调用失败: ${msg}`;
      hilog.error(VERSION_GUARD_DOMAIN, VERSION_GUARD_TAG, 'runHiLogBenchPlatform err %{public}s', msg);
    } finally {
      this.versionGuardBenchPlatformBusy = false;
    }
  }

  private runHiLogFlow(): void {
    if (this.hiLogBusy) {
      return;
    }
    if (!this.bundleName || this.bundleName.length === 0) {
      this.hiLogReport = '未获取到 bundleName（应用包名），请返回后重试。';
      return;
    }
    this.hiLogBusy = true;
    try {
      hilog.info(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG,
        'runHiLog begin dbDir=%{public}s bundle=%{public}s', this.databaseDir, this.bundleName);
      const text: string = lib.runHiLogModuleSmokeTest(this.databaseDir, this.bundleName, this.moduleName);
      this.hiLogReport = text ?? '(无返回内容)';
      hilog.info(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG, 'runHiLog done len=%{public}d', this.hiLogReport.length);
    } catch (e) {
      const msg = `${e}`;
      this.hiLogReport = `调用失败: ${msg}`;
      hilog.error(HI_LOG_CAP_DOMAIN, HI_LOG_CAP_TAG, 'runHiLog err %{public}s', msg);
    } finally {
      this.hiLogBusy = false;
    }
  }

  build() {
    Column() {
      this.DetailBody()
    }
    .width('100%')
    .layoutWeight(1)
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  DetailBody() {
    if (this.moduleKey === 'rdb') {
      Column({ space: 12 }) {
        Button(this.rdbBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.rdbBusy)
          .onClick(() => {
            this.runRdbFlow();
          })

        Scroll() {
          Text(this.rdbReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'common_event') {
      Column({ space: 12 }) {
        Button(this.commonEventBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.commonEventBusy)
          .onClick(() => {
            this.runCommonEventFlow();
          })

        Scroll() {
          Text(this.commonEventReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'huks_key') {
      Column({ space: 12 }) {
        Button(this.huksKeyBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.huksKeyBusy)
          .onClick(() => {
            this.runHuksKeyFlow();
          })

        Scroll() {
          Text(this.huksKeyReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'drawing') {
      Column({ space: 12 }) {
        Button(this.drawingBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.drawingBusy)
          .onClick(() => {
            this.runDrawingFlow();
          })

        Scroll() {
          Text(this.drawingReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'net_connection') {
      Column({ space: 12 }) {
        Button(this.netConnectionBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.netConnectionBusy)
          .onClick(() => {
            this.runNetConnectionFlow();
          })

        Scroll() {
          Text(this.netConnectionReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'hi_app_event') {
      Column({ space: 12 }) {
        Button(this.hiAppEventBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.hiAppEventBusy)
          .onClick(() => {
            this.runHiAppEventFlow();
          })

        Scroll() {
          Text(this.hiAppEventReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'hi_log') {
      Column({ space: 12 }) {
        Button(this.hiLogBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.hiLogBusy)
          .onClick(() => {
            this.runHiLogFlow();
          })

        Scroll() {
          Text(this.hiLogReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'version_guard') {
      Column({ space: 12 }) {
        Column({ space: 6 }) {
          Text(this.versionGuardDeviceBanner)
            .fontSize(12)
            .fontColor('#1E3A5F')
            .width('100%')
        }
        .width('100%')
        .padding(12)
        .backgroundColor('#E3F2FD')
        .borderRadius(8)
        .onAppear(() => {
          this.refreshVersionGuardDeviceBanner();
        })

        Button(this.versionGuardBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.versionGuardBusy)
          .onClick(() => {
            this.runVersionGuardFlow();
          })

        Scroll() {
          Text(this.versionGuardReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else if (this.moduleKey === 'performance') {
      Column({ space: 10 }) {
        Text('性能验证')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
        Text('自封装（cinterop · User.HiLog）')
          .fontSize(13)
          .fontColor('#5D4037')
          .width('100%')
        Button(this.versionGuardBenchCinteropBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.versionGuardBenchCinteropBusy && !this.versionGuardBenchPlatformBusy)
          .onClick(() => {
            this.runVersionGuardHiLogBenchCinteropFlow();
          })
        Scroll() {
          Text(this.versionGuardBenchCinteropReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .width('100%')
        .constraintSize({ minHeight: 120 })
        .backgroundColor('#FFFFFF')
        .borderRadius(6)
        Text('系统库（PerfKit · HiLog）')
          .fontSize(13)
          .fontColor('#5D4037')
          .width('100%')
          .margin({ top: 4 })
        Button(this.versionGuardBenchPlatformBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.versionGuardBenchCinteropBusy && !this.versionGuardBenchPlatformBusy)
          .onClick(() => {
            this.runVersionGuardHiLogBenchPlatformFlow();
          })
        Scroll() {
          Text(this.versionGuardBenchPlatformReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .width('100%')
        .constraintSize({ minHeight: 120 })
        .backgroundColor('#FFFFFF')
        .borderRadius(6)
      }
      .width('100%')
      .padding(12)
      .backgroundColor('#FFF3E0')
      .borderRadius(8)
    } else if (this.moduleKey === 'failure') {
      Column({ space: 12 }) {
        Button(this.failureBusy ? '执行中…' : '运行验证')
          .width('100%')
          .enabled(!this.failureBusy)
          .onClick(() => {
            this.runFailureFlow();
          })

        Scroll() {
          Text(this.failureReport)
            .fontSize(11)
            .fontFamily('monospace')
            .width('100%')
            .padding(12)
        }
        .layoutWeight(1)
        .width('100%')
        .constraintSize({ minHeight: 200 })
        .backgroundColor('#F2F4F7')
        .borderRadius(8)
      }
      .width('100%')
      .height('100%')
      .alignItems(HorizontalAlign.Start)
    } else {
      this.ModuleTodoPlaceholder()
    }
  }

  @Builder
  ModuleTodoPlaceholder() {
    Column({ space: 12 }) {
      Text('该模块尚未接入')
        .fontSize(17)
        .fontWeight(FontWeight.Medium)
      Text('此模块的封装验证能力将陆续补充。')
        .fontSize(14)
        .fontColor('#444444')
        .lineHeight(22)
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
  }
}
