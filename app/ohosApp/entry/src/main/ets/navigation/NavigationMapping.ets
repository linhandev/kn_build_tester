import { preComposeBuilder, PreComposePage } from "@kmp/ability_impl"
import { MainPage, mainPageBuilder } from "../pages/MainPage"


let mapping = new Map<string, WrappedBuilder<Object[]>>()
mapping.set('MainPage', wrapBuilder(mainPageBuilder))
mapping.set('PreComposePage', wrapBuilder(preComposeBuilder))

@Builder
export function pageMap(name: string, param: Object) {
  mapping.get(name)?.builder()
}