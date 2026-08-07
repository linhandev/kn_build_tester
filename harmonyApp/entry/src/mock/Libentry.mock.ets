const NativeMock: Record<string, Object> = {
  'loadFirstModule': (): string => 'mock:k2n',
  'loadSecondModule': (): string => 'mock:n2k',
};

export default NativeMock;
