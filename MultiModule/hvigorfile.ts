import { appTasks } from '@ohos/hvigor-ohos-plugin';
import { HvigorPlugin, HvigorNode } from '@ohos/hvigor';

function customPlugin(): HvigorPlugin {
  return {
    pluginId: 'customPlugin',
    apply(node: HvigorNode) {
      // 插件主体
      console.log('hello customPlugin!');
    }
  }
}

export default {
  system: appTasks, /* Built-in plugin of Hvigor. It cannot be modified. */
  plugins: [customPlugin()]       /* Custom plugin to extend the functionality of Hvigor. */
}