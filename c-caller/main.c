#include <stdio.h>
#include <libc2k_api.h>

int main(void) {
    int sum = add_c_name(10, 3);
    printf("10 + 3 = %d\n", sum);
    return 0;
}
