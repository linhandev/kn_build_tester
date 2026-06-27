import { bundleManager, common } from '@kit.AbilityKit';
import { CAPI_MODULES, CapiModuleItem, findCapiModule } from './capi/CapiModuleRegistry';
import { CapiDetailContent } from './capi/CapiDetailContent';

@Entry
@Component
struct Index {
  /** 空字符串表示模块列表；非空表示当前选中的 moduleKey（不跳转第二页，避免 pageTransition 相关闪退） */
  @State detailModuleKey: string = '';
  @State filesDir: string = '';
  /** 应用沙箱数据库目录，与 RDB SetArea(EL2)+SetDatabaseDir 一致；勿硬编码 el3 路径 */
  @State databaseDir: string = '';
  @State bundleName: string = '';
  @State hapModuleName: string = '';

  aboutToAppear(): void {
    try {
      const ctx = getContext(this) as common.UIAbilityContext;
      this.filesDir = ctx.filesDir;
      this.databaseDir = ctx.databaseDir;
      // UIAbilityContext 类型无 bundleName；用 bundleManager 取包名，失败则与 AppScope/app.json5 一致
      try {
        const bi = bundleManager.getBundleInfoForSelfSync(
          bundleManager.BundleFlag.GET_BUNDLE_INFO_WITH_APPLICATION,
        );
        this.bundleName = bi.name;
      } catch (_) {
        this.bundleName = '';
      }
      if (!this.bundleName || this.bundleName.length === 0) {
        this.bundleName = 'com.example.harmonyapp';
      }
      let mod = 'entry';
      try {
        const hap = ctx.currentHapModuleInfo;
        if (hap && hap.name) {
          mod = hap.name;
        }
      } catch (_) {
      }
      this.hapModuleName = mod;
    } catch (_) {
      this.filesDir = '';
      this.databaseDir = '';
      this.bundleName = '';
      this.hapModuleName = 'entry';
    }
  }

  private detailTitle(): string {
    const meta = findCapiModule(this.detailModuleKey);
    return meta ? meta.title : '未知模块';
  }

  build() {
    Column() {
      if (this.detailModuleKey.length === 0) {
        this.ModuleListView()
      } else {
        this.DetailShellView()
      }
    }
    .height('100%')
    .width('100%')
    .backgroundColor(this.detailModuleKey.length === 0 ? '#FAFAFA' : '#FFFFFF')
  }

  @Builder
  ModuleListView() {
    Column() {
      Column({ space: 6 }) {
        Text('CAPI 封装验证')
          .fontSize(22)
          .fontWeight(FontWeight.Bold)
          .width('100%')
        Text('选择下方模块，进入对应封装验证页面。')
          .fontSize(12)
          .fontColor('#666666')
          .width('100%')
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 16, bottom: 8 })

      List({ space: 0 }) {
        ForEach(CAPI_MODULES, (item: CapiModuleItem) => {
          ListItem() {
            Row() {
              Column({ space: 6 }) {
                Text(item.title)
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#1A1A1A')
                Text(item.subtitle)
                  .fontSize(12)
                  .fontColor('#888888')
                  .maxLines(2)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Text('›')
                .fontSize(20)
                .fontColor('#CCCCCC')
                .margin({ left: 8 })
            }
            .width('100%')
            .padding({
              left: 16,
              right: 16,
              top: 14,
              bottom: 14,
            })
            .border({ width: { bottom: 1 }, color: '#EEEEEE' })
          }
          .onClick(() => {
            this.detailModuleKey = item.key;
          })
        }, (item: CapiModuleItem) => item.key)
      }
      .layoutWeight(1)
      .width('100%')
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  DetailShellView() {
    Column() {
      Row() {
        Row({ space: 4 }) {
          Text('‹')
            .fontSize(26)
            .fontWeight(FontWeight.Medium)
            .fontColor('#2563EB')
            .margin({ top: -2 })
          Text('返回')
            .fontSize(15)
            .fontColor('#2563EB')
        }
        .padding({
          left: 12,
          right: 16,
          top: 10,
          bottom: 10,
        })
        .borderRadius(22)
        .backgroundColor('#EFF6FF')
        .onClick(() => {
          this.detailModuleKey = '';
        })

        Text(this.detailTitle())
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .layoutWeight(1)
          .margin({ left: 12 })
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 10 })
      .alignItems(VerticalAlign.Center)

      Divider().color('#E8E8E8')

      Column() {
        CapiDetailContent({
          moduleKey: this.detailModuleKey,
          filesDir: this.filesDir,
          databaseDir: this.databaseDir,
          bundleName: this.bundleName,
          moduleName: this.hapModuleName,
        })
      }
      .layoutWeight(1)
      .width('100%')
      .justifyContent(FlexAlign.Start)
      .alignItems(HorizontalAlign.Start)
      .padding(16)
    }
    .width('100%')
    .height('100%')
  }
}
