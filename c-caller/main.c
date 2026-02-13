#include <libc2k_api.h>
#include <stdio.h>

// #include "../add/src/nativeInterop/add/add.h"

int main(void) {
  int sum = add_c_name(10, 3);
  printf("10 + 3 = %d\n", sum);
  
  // for cinterop static libs wo export symbol optimization, calling the c impl directly also works
  // printf("2 + 4 = %d\n", addCFun(2, 4));

  return 0;
}
