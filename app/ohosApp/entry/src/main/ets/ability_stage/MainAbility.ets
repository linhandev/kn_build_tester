import AbilityConstant from '@ohos.app.ability.AbilityConstant';
import UIAbility from '@ohos.app.ability.UIAbility';
import Want from '@ohos.app.ability.Want';
import window from '@ohos.window';
import { Configuration } from '@kit.AbilityKit';
import { ComposeInitializer } from '@bytekmp/sample';
import { globalContext } from '@kmp/ability_impl';


export default class EntryAbility extends UIAbility {

  onWindowStageCreate(windowStage: window.WindowStage): void {
    ComposeInitializer.init(this.context, windowStage)
    windowStage.getMainWindow().then(windowClass => {
      globalContext.mainWindow = windowClass
      windowClass.setWindowLayoutFullScreen(true)
      windowClass.on('avoidAreaChange', () => {
        const avoidArea = globalContext.mainWindow!.getWindowAvoidArea(window.AvoidAreaType.TYPE_CUTOUT);
        globalContext.statusBarHeight = avoidArea.topRect.height;
      });
    })
    windowStage.loadContent('pages/MainShell', (err, data) => {
      if (err.code) {
        return;
      }
    });
  }

  onDestroy(): void | Promise<void> {
    globalContext.mainWindow = undefined
  }
}
