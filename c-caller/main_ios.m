#import <Foundation/Foundation.h>
#import <c2k/c2k.h>
#include <stdio.h>

int main() {
    @autoreleasepool {
        int result = [C2kMainKt subtractNumbersA:10 b:3];
        printf("10 - 3 = %d\n", result);
        
        int sum = [C2kMainKt addNumbersA:10 b:3];
        printf("10 + 3 = %d\n", sum);
        
        int product = [C2kMainKt multiplyNumbersWrapperA:10 b:3];
        printf("10 * 3 = %d\n", product);
    }
    return 0;
}

