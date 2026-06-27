/**
 * CAPI 封装验证 — 模块列表（与 Kotlin / NAPI 子模块一一对应，按需扩展）。
 */
export interface CapiModuleItem {
  /** 路由参数 moduleKey */
  key: string;
  /** 列表主标题 */
  title: string;
  /** 副标题 / 说明 */
  subtitle: string;
}

export const CAPI_MODULES: CapiModuleItem[] = [
  { key: 'rdb', title: '模块1：RDB模块', subtitle: '分布式数据管理 · 关系型数据库（RDB）封装验证' },
  { key: 'common_event', title: '模块2：OH_CommonEvent公共事件模块', subtitle: '公共事件订阅 / 发布 / Parameters' },
  { key: 'huks_key', title: '模块3：HuksKeyApi密钥管理模块', subtitle: 'HUKS 密钥管理 · GetSdkVersion / ParamSet（场景一）' },
  { key: 'net_connection', title: '模块4：NetConnection网络连接模块', subtitle: 'HTTP 代理 / QueryProbeResult / 默认网络回调' },
  { key: 'hi_app_event', title: '模块5：HiAppEvent用户打点模块', subtitle: 'ParamList / Write / Watcher 回调与头文件宏比对' },
  { key: 'hi_log', title: '模块6：HiLog日志模块', subtitle: 'DEBUG/INFO/Print/VPrint 与 SetCallback 宏比对' },
  { key: 'drawing', title: '模块7：Drawing模块', subtitle: 'native_drawing：RoundRect / ColorMatrix / Font·Bitmap·Typography / Apple_Chancery 注册' },
  {
    key: 'failure',
    title: '失败场景模块',
    subtitle: '各模块挑 1 个函数，空值/边界值/异常值构造失败返回',
  },
  {
    key: 'version_guard',
    title: '版本检测机制模块',
    subtitle: 'ApiGuard · API18–22 分档调用与版本拦截（函数/常量）',
  },
  {
    key: 'performance',
    title: '性能验证模块',
    subtitle: '自封装/系统库对比验证',
  },
];

export function findCapiModule(key: string): CapiModuleItem | undefined {
  for (let i = 0; i < CAPI_MODULES.length; i++) {
    const m = CAPI_MODULES[i];
    if (m.key === key) {
      return m;
    }
  }
  return undefined;
}
