import { hilog } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  @State message: string = '';

  aboutToAppear(): void {
    try {
      this.message = testNapi.runHelloWorld();
      hilog.info(DOMAIN, 'testTag', 'Kotlin greeting: %{public}s', this.message);
      const hookOk = testNapi.testOhLogHook();
      hilog.info(DOMAIN, 'testTag', 'OH_LOG_PrintMsg x5, HiLog tag LogHookTest shows [wrap #N] …: %{public}s', hookOk ? 'OK' : 'FAIL');
    } catch (err) {
      hilog.error(DOMAIN, 'testTag', 'runHelloWorld failed: %{public}s', JSON.stringify(err));
    }
  }

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            this.message = 'Welcome';
            hilog.info(DOMAIN, 'testTag', 'NAPI add 2 + 3 = %{public}d', testNapi.add(2, 3));
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
