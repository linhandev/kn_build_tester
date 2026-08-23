import { KmpSampleUI, OnBackPressEventBus } from "@bytekmp/sample"

@Component
export struct MainPage {

  backPressEventBus = new OnBackPressEventBus()

  aboutToAppear(): void {
    console.log('MainPage aboutToAppear')
  }

  build() {
    NavDestination() {
      KmpSampleUI({backEventBus: this.backPressEventBus})
    }
    .onBackPressed(() => {
      return this.backPressEventBus.onBackPress()
    })
    .width('100%')
    .height('100%')
  }
}

@Builder
export function mainPageBuilder() {
  MainPage()
}