const NativeMock: Record<string, Object> = {
  'add': (a: number, b: number) => {
    return a + b;
  },
  'runHelloWorld': (): string => 'Hello from Kotlin/Native (mock)',
};

export default NativeMock;