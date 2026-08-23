import { inspector } from "@kit.ArkUI";

export class DrawObserver {
  private context: UIContext
  private listener: inspector.ComponentObserver

  private startTime: number = 0
  private promise ?: Promise<number>
  private resolve ?: (value: number) => void
  private reject ?: (reason: Object) => void
  private canFinish ?: () => boolean

  constructor(context: UIContext, id: string) {
    this.context = context
    this.listener = this.context.getUIInspector().createComponentObserver(id);
  }

  onDrawCompleteCallback = () => {
    console.log(`[PreComposePage] onDrawCompleteCallback`)
    if (this.canFinish && this.canFinish() && this.resolve) {
      const endTime = new Date().getTime();
      this.resolve(endTime - this.startTime)
      this.clear()
    }
  };

  startListen(): void {
    this.listener.on('draw', this.onDrawCompleteCallback);
  }

  stopListen() {
    this.listener.off('draw', this.onDrawCompleteCallback);
  }

  record(canFinish: () => boolean): Promise<number> {
    if (this.reject) {
      this.reject("")
    }
    this.clear()
    this.canFinish = canFinish

    this.startTime = new Date().getTime()
    this.promise =  new Promise((resolve, reject) => {
      this.resolve = resolve
      this.reject = reject
    })
    return this.promise
  }

  clear() {
    this.promise = undefined
    this.resolve = undefined
    this.reject = undefined
    this.canFinish = undefined
  }
}