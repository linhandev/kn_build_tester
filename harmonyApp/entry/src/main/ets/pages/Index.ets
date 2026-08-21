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
      this.message = 'FAIL arkts ' + JSON.stringify(err);
      hilog.error(DOMAIN, 'cname-eb', '%{public}s', this.message);
      return;
    }
    setTimeout(() => {
      hilog.info(DOMAIN, 'cname-eb', 'calling kn_cname_throw (expect process death)');
      testNapi.runCnameThrow();
    }, 2500);
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
