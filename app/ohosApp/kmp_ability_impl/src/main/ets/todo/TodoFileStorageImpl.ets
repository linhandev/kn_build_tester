import { ITodoFileStorage } from "@bytekmp/sample";
import fs from "@ohos.file.fs"
import { globalContext } from "../../../../Index";

class TodoFileStorageImpl implements ITodoFileStorage {

  private fileName ?: string

  async getContent(): Promise<string | undefined> {
    if (await fs.access(this.getFileName())) {
      return await fs.readText(this.getFileName())
    } else {
      return undefined
    }

  }
  async saveContent(content: string): Promise<void> {
    await this.createFile()
    let fd = (await fs.open(this.getFileName(), fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC
    )).fd
    await fs.write(fd, content)
  }

  async createFile() {
    if (!(await fs.access(this.getFileName()))) {
      await fs.open(this.getFileName(), fs.OpenMode.CREATE)
    }
  }

  private getFileName(): string {
    if (!this.fileName && globalContext.context) {
      this.fileName = globalContext.context!.cacheDir + "/bytekmp_todo.json"
    }
    return this.fileName!
  }
}

export const instanceOfITodoFileStorage = new TodoFileStorageImpl()