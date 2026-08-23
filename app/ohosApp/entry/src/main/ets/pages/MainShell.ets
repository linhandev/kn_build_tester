import { KmpSampleUI } from "@bytekmp/sample";
import { globalContext } from "@kmp/ability_impl";
import { pageMap } from "../navigation/NavigationMapping";
import { MainPage } from "./MainPage";

/**
 * 页面 Navigation 顶层跳转容器
 * @author : dongchen.zoyp
 * @date   : 2024/4/28
 */
@Entry
@Component
export struct MainShell {
  navPathStack: NavPathStack = new NavPathStack();

  aboutToAppear(): void {
    globalContext.uiContext = this.getUIContext()
    globalContext.context = getContext()
    globalContext.navPathStack = this.navPathStack
  }

  aboutToDisappear(): void {
    globalContext.uiContext = undefined
    globalContext.navPathStack = undefined
  }

  build() {
    Stack() {
      Navigation(this.navPathStack)
        .onAppear(async () => {
          // 默认展示主页
          this.navPathStack.pushPath({name: "MainPage"})
        })
        .hideToolBar(true)
        .hideTitleBar(true)
        .hideNavBar(true)
        .hideBackButton(true)
        .navDestination(pageMap)
        .mode(NavigationMode.Stack)
        .width('100%')
        .height('100%')
    }
    .width('100%')
    .height('100%')
  }
}
