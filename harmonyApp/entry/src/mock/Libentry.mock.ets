const NativeMock: Record<string, Object> = {
  'runHelloWorld': (): string => 'PASS mock',
  'runCnameThrow': (): void => {},
};

export default NativeMock;
