import { IPreComposeSample } from '@bytekmp/sample';
import { globalContext } from '../../../../Index';


class PreComposeSampleImpl implements IPreComposeSample {
  enablePreCompose(): boolean {
    return true
}
  openPreComposePage() {
    globalContext.navPathStack?.pushPath({ name: 'PreComposePage' })
  }

}

export const instanceOfIPreComposeSample = new PreComposeSampleImpl()