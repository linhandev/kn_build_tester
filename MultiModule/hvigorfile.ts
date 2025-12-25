import { appTasks } from '@ohos/hvigor-ohos-plugin';
import { HvigorPlugin, HvigorNode } from '@ohos/hvigor';

function customPlugin(): HvigorPlugin {
  return {
    pluginId: 'customPlugin',
    apply(node: HvigorNode) {
      console.log(process.env.config)
      // Only print if not in module mode (config does not contain "module")
      if (!process.env.config?.includes('"module"')) {
        console.log('hello customPlugin!');
      }
    }
  }
}

export default {
  system: appTasks, /* Built-in plugin of Hvigor. It cannot be modified. */
  plugins: [customPlugin()] /* Custom plugin to extend the functionality of Hvigor. */
}