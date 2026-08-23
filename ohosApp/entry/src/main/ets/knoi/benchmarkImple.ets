import { BenchMarkConsumer, BenchMarkConsumerForWord } from './consumer';
import { List } from '@kit.ArkTS';

export class BenchmarkImpl implements BenchMarkConsumer{
  res = new Uint8Array()

  constructor() {
    this.res = getContext().resourceManager.getMediaByNameSync("icon")
  }

  benchUnit(): void {
  }

  benchBoolean(boolean: boolean): boolean {
    return boolean
  }

  benchInt(int: number): number {
    return int

  }

  benchLong(long: number): number {
    return long
  }

  benchDouble(double: number): number {
    return double
  }

  benchString(string: string): string {
    return string
  }

  benchArrayBuffer(): Uint8Array {
    return this.res
  }

  benchList(list: void): void {
    return list
  }

  benchJSCallback(jsCallback: Function): Function {
    return jsCallback
  }

  benchMap(map: Map<string,object>): Map<string,object> {
    return map
  }

}

export class BenchmarkForwordImpl implements BenchMarkConsumerForWord{
  benchUnit(): void {
  }

  benchBoolean(boolean: boolean): void {
  }

  benchInt(int: number): void {
  }

  benchLong(long: number): void {
  }

  benchDouble(double: number): void {
  }

  benchString(string: string): void {
  }

  benchList(list: void): void {
  }

  benchJSCallback(jsCallback: Function): void {
  }

  benchMap(map: object): void {
  }


}
