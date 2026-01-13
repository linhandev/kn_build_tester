import { hilog, hidebug } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;
testNapi.testGcov();

@Entry
@Component
struct Index {
  build() {
    Row() {
      Column() {
        Text("Hello")
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
        Button("Run Gcov Test")
          .onClick(() => {
            hilog.info(0x0000, 'testTag', 'Running Gcov Test');            
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
