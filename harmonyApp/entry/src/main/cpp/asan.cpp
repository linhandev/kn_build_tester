#include "asan.h"
#include <string.h>
#include <stdio.h>

extern "C" {

void trigger_overflow(char* buffer, int size, int overflow_amount) {
    printf("C: Received buffer at %p, size %d, overflow %d\n", buffer, size, overflow_amount);
    // Intentionally write past the end
    for (int i = 0; i < size + overflow_amount; ++i) {
        buffer[i] = 'A'; 
    }
    printf("C: Finished writing\n");
}

}
