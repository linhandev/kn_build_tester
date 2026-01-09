import { hilog, hidebug } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;
hilog.info(DOMAIN, 'testTag', 'Test NAPI 2 + 3 = %{public}d', testNapi.add(2, 3));

let options: hidebug.GwpAsanOptions = {
  alwaysEnabled: true,
  sampleRate: 1,
  maxSimutaneousAllocations: 1000,
};

try {
  hidebug.enableGwpAsanGrayscale(options)
} catch (error) {
  // TODO: Implement error handling.
}

@Entry
@Component
struct Index {
  @State message: string = String(testNapi.add(2, 3));

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize($r('app.float.page_text_font_size'))
          .fontWeight(FontWeight.Bold)
        Button("Run ASan Test")
          .onClick(() => {
            hilog.info(0x0000, 'testTag', 'Running ASan Test');
            testNapi.testAsan();
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
