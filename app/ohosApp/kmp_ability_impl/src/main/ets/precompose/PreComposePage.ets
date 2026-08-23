import {
  disposePreCompose,
  enableFrameMonitor,
  NormalComposeSample, OnBackPressEventBus,
  OnDisposeCollection,
  preCompose,
  PreComposeParam, PreComposeSample,
  PreloadHandler } from "@bytekmp/sample"
import { inspector, window } from "@kit.ArkUI";
import { globalContext } from "../context/GlobalContext";
import { DrawObserver } from "./DrawObserver";

@Component
export struct PreComposePage {
  @State selectedValue: SelectOption = {value: '模拟网络请求'}
  selectedOptionIndex: number = 0
  private options: SelectOption[] = [{value: '模拟网络请求'}, {value: '模拟耗时'}]

  @State currentTabIndex: number = 0
  @State showNormalCompose: boolean = false
  @State showPreCompose: boolean = false

  private normalStartTime: number = 0
  @State normalCostTime: number = 0

  private preComposeStartTime: number = 0
  @State preComposeCostTime: number = 0
  @State private preloadHandler ?: PreloadHandler = undefined

  private composeSize = 180

  aboutToAppear(): void {
    enableFrameMonitor(true)
  }

  build() {
    NavDestination() {
      this.content()
    }
    .hideToolBar(true)
    .hideTitleBar(true)
    .hideBackButton(true)
    .padding({top: globalContext.statusBarHeight})
    .width('100%')
    .height('100%')
  }

  @Builder
  content() {
    Column() {
      Row() {
        Text("选择测试场景 ")
        Select(this.options)
          .value(this.selectedValue.value)
          .onSelect((index: number) => {
            this.selectedOptionIndex = index
            this.selectedValue = this.options[index]
            this.showNormalCompose = false
            this.showPreCompose = false
            this.normalStartTime = 0
            this.normalCostTime = 0
            this.preComposeStartTime = 0
            this.preComposeCostTime = 0
            disposePreCompose(this.preloadHandler?.getUuid() ?? "")
            this.preloadHandler = undefined
          })
      }
      Tabs() {
        TabContent() {
          Column() {
            Text(`加载耗时: ${this.normalCostTime} ms`)
            Stack() {
              this.normalComposeUI()
            }
            .width(this.composeSize)
            .height(this.composeSize)
          }
          // .width(this.composeSize + 20)
          // .height(this.composeSize + 20)
          .border({
            width: 2,
            color: Color.Black,
            radius: 8
          })
        }.tabBar(this.normalComposeTabBar())
        TabContent() {
          Column() {
            Text(`加载耗时: ${this.preComposeCostTime} ms`)
            Stack() {
              this.preComposeUI()
            }
            .width(this.composeSize)
            .height(this.composeSize)
          }
          // .width(this.composeSize + 20)
          // .height(this.composeSize + 20)
          .border({
            width: 2,
            color: Color.Black,
            radius: 8
          })
        }.tabBar(this.preComposeTabBar())
      }
      .onChange((index: number) => {
        this.currentTabIndex = index
      })
      .layoutWeight(1)
      .width('100%')
      .padding(3)
      .border({
        width: 2,
        color: Color.Black,
        radius: 8
      })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 10, right: 10, bottom: 20 })
  }

  @Builder
  normalComposeTabBar() {
    Text("Normal Compose")
      .backgroundColor(this.currentTabIndex == 0 ? 0xFFE9E9E8 : Color.White)
      .padding(10)
      .borderRadius(10)
  }

  @Builder
  normalComposeUI() {
    if (this.showNormalCompose) {
      NormalComposeSample(new PreComposeParam(this.selectedOptionIndex, {
        onDrawFrame: (time) => {
          console.log(`[PreComposePage] onDrawFrame ${time - this.normalStartTime}`)
          this.normalCostTime = time - this.normalStartTime
        }
      }))
    } else {
      Text("点击加载 ComposeView")
        .onClick(_ => {
          console.log(`[PreComposePage] start show normal compose`)
          this.normalStartTime = new Date().getTime()
          this.showNormalCompose = true
        })
        .width('100%')
        .height('100%')
    }
  }

  @Builder
  preComposeTabBar() {
    Text("PreCompose")
      .backgroundColor(this.currentTabIndex == 1 ? 0xFFE9E9E8 : Color.White)
      .padding(10)
      .borderRadius(10)
  }

  @Builder
  preComposeUI() {
    if (this.showPreCompose) {
      PreComposeSample(new PreComposeParam(this.selectedOptionIndex, undefined))
    } else if (!this.preloadHandler) {
      Text("点击触发 PreCompose")
        .onClick(_ => {
          this.preloadHandler = preCompose(
            'PreComposeSample',
            0,
            {},
            new PreComposeParam(this.selectedOptionIndex, {
              onDrawFrame: (time) => {
                console.log(`[PreComposePage] onDrawFrame ${time - this.preComposeStartTime}`)
                this.preComposeCostTime = time - this.preComposeStartTime
              }
            }),
            // 确保 size 准确
            {width: vp2px(this.composeSize), height: vp2px(this.composeSize)}
          )
        })
        .width('100%')
        .height('100%')
    } else {
      Text("PreCompose 触发成功，等待3s后点击加载 ComposeView")
        .onClick(_ => {
          console.log(`[PreComposePage] start show precompose`)
          this.preloadHandler?.markPreloaded()
          this.preComposeStartTime = new Date().getTime()
          this.showPreCompose = true
          this.preloadHandler = undefined
        })
        .width('100%')
        .height('100%')
    }
  }

}

@Builder
export function preComposeBuilder() {
  PreComposePage()
}

