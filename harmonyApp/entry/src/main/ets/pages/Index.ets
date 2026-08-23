import { hilog } from '@kit.PerformanceAnalysisKit';
import demo from 'libentry.so';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  @State result: string = '';

  build() {
    Row() {
      Column({ space: 16 }) {
        Button('greet')
          .width('80%').height(48)
          .onClick(() => { this.result = demo.greet('World'); })
        Button('Counter')
          .width('80%').height(48)
          .onClick(() => {
            const c = new demo.Counter();
            const a = c.increment();
            const b = c.increment();
            c.reset();
            this.result = `inc=${a},inc2=${b},after reset=${c.getCount()}`;
          })
        Text(this.result)
          .width('80%').fontSize(16).margin({ top: 10 })
      }
      .width('100%')
    }
    .height('100%')
  }

  aboutToAppear(): void {
    try {
      this.result = demo.greet('akinterop');
      hilog.info(DOMAIN, 'akinterop', 'greet: %{public}s', this.result);
    } catch (err) {
      hilog.error(DOMAIN, 'akinterop', 'greet failed: %{public}s', JSON.stringify(err));
    }
  }
}
