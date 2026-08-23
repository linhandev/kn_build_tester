import window from "@ohos.window"
import { Context } from "@kit.AbilityKit"

class GlobalContext {
  mainWindow ?: window.Window
  uiContext ?: UIContext
  context ?: Context
  navPathStack ?: NavPathStack
  statusBarHeight: number = 0
}

export const globalContext = new GlobalContext()