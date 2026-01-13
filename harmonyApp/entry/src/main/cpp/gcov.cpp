#include "include/gcov.h"
#include <stdio.h>
#include <cstdlib>

void test_gcov_c() {
    // int value = rand() % 3;
    int value = 2;
    
    switch (value) {
        case 1:
            printf("1\n");
            break;
        case 2:
            printf("2\n");
            break;
        default:
            printf("default\n");
            break;
    }
}
