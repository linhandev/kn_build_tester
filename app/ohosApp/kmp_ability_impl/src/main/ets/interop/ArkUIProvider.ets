import { ArkUIWithCallback, IArkUIProvider } from '@bytekmp/sample';
import { ComponentContent } from '@kit.ArkUI';
import { globalContext } from '../../../../Index';

class ArkUIProviderImpl implements IArkUIProvider {
  getArkUIComponentContent(): ArkUIWithCallback {
    let result = new ArkUIWithCallback()
    let totalInput = ''
    let componentContent = new ComponentContent<string>(globalContext.uiContext!, wrapBuilder(ArkUIText), totalInput)
    result.setComponentContent(componentContent)
    result.addCallback({
      onInputUpdate: (input: string) => {
        totalInput += input
        componentContent.update(totalInput)
      }
    })
		return result
	}
}

@Builder
function ArkUIText(content: string) {
  Column() {
    Text(content)
  }.width('100%')
  .height('100%')
  .backgroundColor(Color.Yellow)
}

export const instanceOfIArkUIProvider = new ArkUIProviderImpl()