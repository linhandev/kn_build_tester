#ifndef ASAN_TEST_H
#define ASAN_TEST_H

#ifdef __cplusplus
extern "C" {
#endif

void trigger_overflow(char* buffer, int size, int overflow_amount);

#ifdef __cplusplus
}
#endif

#endif
