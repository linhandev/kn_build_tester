#include "include/gcov.h"
#include <stdio.h>

void test_gcov_c() {
    switch (1) {
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

