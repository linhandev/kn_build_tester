import { hilog } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  @State message: string = 'Hello World';

  aboutToAppear(): void {
    const tzMsg: string = testNapi.getTimeZoneRulesViaI18n();
    hilog.info(DOMAIN, 'testTag', '[auto] GetTimeZoneRules %{public}s', tzMsg);
    this.message = tzMsg;
  }

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            this.message = 'Welcome';
            testNapi.runHelloWorld();
            hilog.info(DOMAIN, 'testTag', 'NAPI add 2 + 3 = %{public}d', testNapi.add(2, 3));
            const tzMsg: string = testNapi.getTimeZoneRulesViaI18n();
            hilog.info(DOMAIN, 'testTag', 'GetTimeZoneRules %{public}s', tzMsg);
            this.message = tzMsg;
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
