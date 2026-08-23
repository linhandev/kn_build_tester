import libs from "libknoi.so"
import { taskpool } from '@kit.ArkTS';

export function executeSync<R> (obj: object): R {
  let id: number = libs.create_function_waiter();
  let task = new taskpool.Task(execute_function, obj, id);
  let array = new Array<Object>();
  array.push(obj);
  task.setCloneList(array);
  taskpool.execute(task);

  return JSON.parse(libs.wait_on_function_waiter(id)) as R;
}

@Concurrent
export function execute_function(object: object, id: number): void {
  let result = object["invoke"]() as Promise<object>;

  result.then((data: object) => {
    let result = JSON.stringify(data)
    libs.notify_function_waiter(id, result, result.length)
  }, (err: object) => {
    let result = JSON.stringify(err)
    libs.notify_function_waiter(id, result, result.length)
  })
}