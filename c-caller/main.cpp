#include <stdio.h>
#include <libc2k_api.h>

int main() {
    int result = subtract_numbers(10, 3);
    printf("10 - 3 = %d\n", result);
    int sum = add_numbers(10, 3);
    printf("10 + 3 = %d\n", sum);
    int product = multiply_numbers(10, 3);
    printf("10 * 3 = %d\n", product);
    return 0;
}