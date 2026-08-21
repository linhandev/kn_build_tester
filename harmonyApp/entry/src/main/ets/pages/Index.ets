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
      hilog.info(DOMAIN, 'cname-eb', '%{public}s', this.message);
    } catch (err) {
      hilog.error(DOMAIN, 'cname-eb', 'runHelloWorld failed: %{public}s', JSON.stringify(err));
    }
  }

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
      }
      .width('100%')
    }
    .height('100%')
  }
}
