import { hilog } from '@kit.PerformanceAnalysisKit';
import testNapi from 'libentry.so';

const DOMAIN = 0x0000;

@Entry
@Component
struct Index {
  @State firstMsg: string = 'loading k2n.so …';
  @State secondMsg: string = 'tap anywhere to load n2k.so';
  @State secondLoaded: boolean = false;

  aboutToAppear(): void {
    try {
      this.firstMsg = testNapi.loadFirstModule();
      hilog.info(DOMAIN, 'ondemand', 'first module: %{public}s', this.firstMsg);
    } catch (err) {
      this.firstMsg = 'loadFirstModule failed: ' + JSON.stringify(err);
      hilog.error(DOMAIN, 'ondemand', '%{public}s', this.firstMsg);
    }
  }

  private loadSecond(): void {
    if (this.secondLoaded) {
      return;
    }
    try {
      this.secondMsg = testNapi.loadSecondModule();
      this.secondLoaded = true;
      hilog.info(DOMAIN, 'ondemand', 'second module: %{public}s', this.secondMsg);
    } catch (err) {
      this.secondMsg = 'loadSecondModule failed: ' + JSON.stringify(err);
      hilog.error(DOMAIN, 'ondemand', '%{public}s', this.secondMsg);
    }
  }

  build() {
    Column() {
      Text('4-SO on-demand (runtime/std + k2n/n2k)')
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 16 })
      Text(this.firstMsg)
        .fontSize(16)
        .margin({ bottom: 12 })
      Text(this.secondMsg)
        .fontSize(16)
    }
    .width('100%')
    .height('100%')
    .padding(24)
    .justifyContent(FlexAlign.Center)
    .onClick(() => {
      this.loadSecond();
    })
  }
}
