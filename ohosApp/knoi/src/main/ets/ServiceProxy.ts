import { callService } from './knoi';

export function getService<R>(service: string): R {

  const target = {}
  return new Proxy(target, {
    get: (target, prop, receiver) => {
      return function (...args) {
        return callService(service, target, String(prop), ...args);
      };
    }
  }) as R;
}