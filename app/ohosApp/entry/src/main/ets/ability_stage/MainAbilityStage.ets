import { onDensityPixelsChanged, setResourceManager } from '@bytekmp/sample';
import AbilityStage from '@ohos.app.ability.AbilityStage';
import { display } from '@kit.ArkUI';

export default class MainAbilityStage extends AbilityStage {

  onCreate(): void {
    let resManager = this.context.resourceManager
    setResourceManager(resManager)
    let dpi = display.getDefaultDisplaySync().densityPixels
    onDensityPixelsChanged(dpi)
  }
}