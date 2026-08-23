/***
*    !!!  GEN CODE DO NOT EDIT  !!!
***/
export interface TestServiceAInCommon {
            
   methodWithIntReturnInt(a: number): number;

}

export interface BenchMarkConsumer {
            
   benchUnit(): void;

   benchBoolean(boolean: boolean): boolean;

   benchInt(int: number): number;

   benchLong(long: number): number;

   benchDouble(double: number): number;

   benchString(string: string): string;

   benchArrayBuffer(): ArrayBuffer;

   benchList(list: void): void;

   benchJSCallback(jsCallback: Function): Function;

   benchMap(map: object): object;

}

export interface BenchMarkConsumerForWord {
            
   benchUnit(): void;

   benchBoolean(boolean: boolean): void;

   benchInt(int: number): void;

   benchLong(long: number): void;

   benchDouble(double: number): void;

   benchString(string: string): void;

   benchList(list: void): void;

   benchJSCallback(jsCallback: Function): void;

   benchMap(map: object): void;

}

export interface SingletonTestServiceA {
            
   method1(str: string): void;

   method2(str: string): string | null;

}

export interface TestServiceA {
            
   methodWithIntReturnInt(a: number): number;

   methodWithLongReturnLong(a: number): number;

   methodWithBooleanReturnBoolean(a: boolean): boolean;

   methodWithDoubleReturnDouble(a: number): number;

   methodWithStringReturnString(a: string): string | null;

   methodWithCallbackReturnCallback(a: Function): Function;

   methodWithCallbackReturnCallback2(a: Function, b: string, c: number): Function;

   methodWithCallbackReturnCallback3(a: Function, b: Function): string;

   methodWithCallbackReturnCallback4(a: Function): string;

   methodWithArrayStringReturnArrayString(a: Array<string>): Array<string>;

   methodWithMapReturnMap(a: object): object;

   methodWithUnit(): void;

   methodWithJSValueReturnJSValue(a: object): object;

   methodWithArrayBufferReturnArrayBuffer(a: ArrayBuffer): ArrayBuffer | null;

   methodWithArrayBufferReturnArrayBufferUseByteArray(a: ArrayBuffer): ArrayBuffer;

   method3Params(a: string, b: number, c: object): object;

   methodWithException(a: string): object;

   methodWithPromise(a: string): object;

}

export interface TestServiceAInWorker {
            
   methodWithIntReturnInt(a: number): number;

   methodWithLongReturnLong(a: number): number;

   methodWithBooleanReturnBoolean(a: boolean): boolean;

   methodWithDoubleReturnDouble(a: number): number;

   methodWithStringReturnString(a: string): string;

   methodWithCallbackReturnCallback(a: Function): Function;

   methodWithArrayStringReturnArrayString(a: Array<string>): Array<string>;

   methodWithMapReturnMap(a: object): object;

   methodWithUnit(): void;

   methodWithJSValueReturnJSValue(a: object): object;

   methodWithArrayBufferReturnArrayBuffer(a: ArrayBuffer): ArrayBuffer | null;

   method3Params(a: string, b: number, c: object): object;

}

