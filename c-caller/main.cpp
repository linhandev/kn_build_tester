#include <stdio.h>
#include <libc2k_api.h>

int main() {
    int sum = add_numbers(10, 3);
    printf("10 + 3 = %d\n", sum);
    return 0;
}
