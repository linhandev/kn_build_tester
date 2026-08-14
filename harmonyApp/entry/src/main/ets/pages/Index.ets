import { hilog } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  @State message: string = '';

  aboutToAppear(): void {
    // cold-start UAF: exercise libc2k.so HWASAN quarantine bake (no HWASAN_OPTIONS)
    hilog.info(DOMAIN, 'testTag', 'aboutToAppear runUafCallback');
    testNapi.runUafCallback();
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
        Button('mallocHeap kotlinCallback UAF')
          .margin({ top: 24 })
          .onClick(() => {
            hilog.info(DOMAIN, 'testTag', 'runUafCallback');
            testNapi.runUafCallback();
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
