#include <stdio.h>
#include <stdlib.h>
#include <libktdemo_api.h>

// CRITICAL: Set GCOV environment before any global constructors run
// This constructor runs before main() and before Kotlin library initialization
__attribute__((constructor))
static void setup_gcov() {
    setenv("GCOV_PREFIX", "/data/local/tmp/gcov_c_driver", 1);
    setenv("GCOV_PREFIX_STRIP", "99", 1);
}

int main() {
    printf("=== C Driver with Linked Kotlin .so (GCOV) ===\n\n");
    printf("[C] GCOV_PREFIX=%s\n", getenv("GCOV_PREFIX"));
    
    int result = compute_factorial(5);
    printf("[C] factorial(5) = %d\n", result);
    
    bool prime_check = is_prime(7);
    printf("[C] is_prime(7) = %s\n", prime_check ? "true" : "false");
    
    prime_check = is_prime(8);
    printf("[C] is_prime(8) = %s\n", prime_check ? "true" : "false");
    
    printf("\n[C] ✅ Test completed. .gcda will be written on exit.\n");
    
    return 0;
}
