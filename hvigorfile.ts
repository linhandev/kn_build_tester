import { appTasks, rootNode } from '@ohos/hvigor-ohos-plugin';
import { hvigor, getNode, HvigorPlugin, HvigorNode } from '@ohos/hvigor';

// 只在sync时执行一次的任务
const rootNode = getNode(__filename);
hvigor.nodesEvaluated(() => {
  rootNode.registerTask({
    name:'onceByDependency',
    run() {
      console.log("onceByDependency")
    },
    dependencies:['entry:init'],
    postDependencies:['init']
  })
})


// ！！！ 不是标准的api，不推荐 ！！！
function onceByModuleName(): HvigorPlugin {
  return {
    pluginId: 'onceByModuleName',
    apply(node: HvigorNode) {
      console.log("Config: ", process.env.config)
      if (!process.env.config?.includes('"module"')) {
        console.log('onceByModuleName');
      }
    }
  }
}

export default {
  system: appTasks, /* Built-in plugin of Hvigor. It cannot be modified. */
  plugins: [onceByModuleName()] /* Custom plugin to extend the functionality of Hvigor. */
}