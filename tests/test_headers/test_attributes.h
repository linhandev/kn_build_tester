#ifndef TEST_ATTRIBUTES_H
#define TEST_ATTRIBUTES_H

/* __attribute__((deprecated)) */
__attribute__((deprecated("use new_func instead")))
void old_func(void);

/* __attribute__((visibility("default"))) */
__attribute__((visibility("default")))
void visible_func(void);

/* __attribute__((format)) for printf-like functions */
__attribute__((format(printf, 1, 2)))
int log_message(const char *fmt, ...);

/* __attribute__((noreturn)) — placed inline so source text includes it */
void __attribute__((noreturn)) abort_now(void);

/* __attribute__((constructor)) — inline for source extent capture */
void __attribute__((constructor)) module_init(void);

/* __attribute__((const)) — pure mathematical function */
__attribute__((const))
int square(int x);

#endif
