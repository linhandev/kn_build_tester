import * as lib from 'libentry.so';
import promptAction from '@ohos.promptAction';

function showApiTooHighToast(): void {
  try { promptAction.showToast({ message: '当前API版本过高，请升级系统后再进行操作' }); } catch (_) {}
}

export function safeNumberCall(invoke: () => number): number {
  try {
    const r: number = invoke();
    if (r === -10001) {
      showApiTooHighToast();
    }
    return r;
  } catch (e) {
    try { promptAction.showToast({ message: '调用失败' }); } catch (_) {}
    return -1;
  }
}

export function safeStringCall(invoke: () => string): string {
  try {
    const r: string = invoke();
    if (r === 'API_VERSION_TOO_HIGH') {
      showApiTooHighToast();
    }
    return r;
  } catch (e) {
    try { promptAction.showToast({ message: '调用失败' }); } catch (_) {}
    return 'error';
  }
}

export function safeStringCallSilent(invoke: () => string): string {
  try {
    const r: string = invoke();
    return r;
  } catch (e) {
    return 'error';
  }
}

export function safeBooleanCall(invoke: () => boolean): boolean {
  try {
    const r: boolean = invoke();
    return r;
  } catch (e) {
    try { promptAction.showToast({ message: '调用失败' }); } catch (_) {}
    return false;
  }
}

export function safeDoubleCall(invoke: () => number): number {
  try {
    const r: number = invoke();
    if (r === -10001) {
      showApiTooHighToast();
    }
    return r;
  } catch (e) {
    try { promptAction.showToast({ message: '调用失败' }); } catch (_) {}
    return -1;
  }
}
