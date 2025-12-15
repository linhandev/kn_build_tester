#include <iostream>
#include <sys/mman.h>
#include <unistd.h>
#include <stdexcept>
#include <cstdint>
#include <signal.h>
#include <setjmp.h>
#include <cstring>

// --- OHOS Logging Support ---
// Uncomment to use OH_LOG_Print instead of printf.
// Ensure you link with -lhilog_ndk.z when compiling for OHOS.
// #define USE_OHOS_LOG

#ifdef USE_OHOS_LOG
#include <hilog/log.h>
#undef LOG_DOMAIN
#undef LOG_TAG
#define LOG_DOMAIN 0x0000
#define LOG_TAG "ProtectedBuffer"
// Map printf to OH_LOG_Print.
// Note: HiLog treats arguments as private by default. Use %{public} in format strings if needed.
#define printf(fmt, ...) OH_LOG_Print(LOG_APP, LOG_INFO, LOG_DOMAIN, LOG_TAG, fmt, ##__VA_ARGS__)
#endif
// ----------------------------

enum class ProtectionType {
    NoGuard,
    Underflow,
    Overflow
};

class ProtectedBuffer {
private:
    void* actual_start = nullptr;
    size_t actual_size = 0;
    void* user_start = nullptr;

public:
    ProtectedBuffer(size_t size, ProtectionType type) {
        if (size == 0) return;

        size_t page_size = sysconf(_SC_PAGESIZE);

        // Calculate pages needed for data
        size_t data_pages = (size + page_size - 1) / page_size;
        
        // We need at least one guard page unless NoGuard is requested.
        size_t total_pages = data_pages;
        if (type != ProtectionType::NoGuard) {
            total_pages += 1;
        }
        actual_size = total_pages * page_size;

        // Use actual_start directly
        actual_start = mmap(NULL, actual_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (actual_start == MAP_FAILED) {
            throw std::runtime_error("mmap failed");
        }

        void* guard_page = nullptr;

        if (type == ProtectionType::Overflow) {
            // Layout: [ DATA ... ] [ GUARD ]
            // Guard is the LAST page(s).
            guard_page = (char*)actual_start + (data_pages * page_size);
            
            // User pointer is calculated such that it ends at guard_page
            user_start = (char*)guard_page - size;
            
        } else if (type == ProtectionType::Underflow) {
            // Layout: [ GUARD ] [ DATA ... ]
            // Guard is the FIRST page.
            guard_page = actual_start;
            
            // User pointer is the start of the second page (or just after guard)
            user_start = (char*)actual_start + page_size;
        } else { // NoGuard
            // Layout: [ DATA ... ]
            // Just page aligned user data.
            user_start = actual_start;
        }

        // Protect the guard page if needed
        if (type != ProtectionType::NoGuard) {
            if (mprotect(guard_page, page_size, PROT_NONE) == -1) {
                munmap(actual_start, actual_size);
                throw std::runtime_error("mprotect failed");
            }
        }
    }

    ~ProtectedBuffer() {
        if (actual_start && actual_start != MAP_FAILED) {
            munmap(actual_start, actual_size);
        }
    }

    void* get() const {
        return user_start;
    }

    // Prevent copying
    ProtectedBuffer(const ProtectedBuffer&) = delete;
    ProtectedBuffer& operator=(const ProtectedBuffer&) = delete;
};

// --- Test Code ---

sigjmp_buf jump_buffer;

void signal_handler(int sig) {
    if (sig == SIGSEGV || sig == SIGBUS) {
        printf("[Test] Caught signal %d!\n", sig);
        siglongjmp(jump_buffer, 1);
    }
}

int main() {
    setvbuf(stdout, NULL, _IONBF, 0);
    signal(SIGSEGV, signal_handler);
    signal(SIGBUS, signal_handler);

    size_t size = 64;

    printf("--- Testing Overflow Protection ---\n");
    {
        ProtectedBuffer buffer(size, ProtectionType::Overflow);
        void* p1 = buffer.get();
        printf("Allocated %zu bytes at %p (Overflow Protected)\n", size, p1);

        // Legal access
        ((char*)p1)[size - 1] = 'A';
        printf("Legal access at size-1 OK.\n");

        // Illegal access
        printf("Attempting overflow at size (should crash)...\n");
        if (sigsetjmp(jump_buffer, 1) == 0) {
            ((char*)p1)[size] = 'X';
            printf("FAILED: Overflow not detected!\n");
        } else {
            printf("SUCCESS: Overflow detected!\n");
        }
    } // buffer destroyed here

    printf("\n--- Testing Underflow Protection ---\n");
    {
        ProtectedBuffer buffer(size, ProtectionType::Underflow);
        void* p2 = buffer.get();
        printf("Allocated %zu bytes at %p (Underflow Protected)\n", size, p2);

        // Legal access
        ((char*)p2)[0] = 'B';
        printf("Legal access at 0 OK.\n");

        // Illegal access
        printf("Attempting underflow at -1 (should crash)...\n");
        if (sigsetjmp(jump_buffer, 1) == 0) {
            ((char*)p2)[-1] = 'Y';
            printf("FAILED: Underflow not detected!\n");
        } else {
            printf("SUCCESS: Underflow detected!\n");
        }
    } // buffer destroyed here

    printf("\n--- Testing NoGuard Protection ---\n");
    {
        ProtectedBuffer buffer(size, ProtectionType::NoGuard);
        void* p3 = buffer.get();
        printf("Allocated %zu bytes at %p (No Guard)\n", size, p3);

        // Legal access
        ((char*)p3)[0] = 'C';
        ((char*)p3)[size - 1] = 'D';
        printf("Legal access at 0 and size-1 OK.\n");

        // Illegal access (Logical overflow)
        // Since this is NoGuard, and size (64) < page size, this memory IS accessible.
        // Without manual ASan poisoning, this should NOT crash and NOT be detected.
        printf("Attempting logical overflow at size (should NOT crash)...\n");
        if (sigsetjmp(jump_buffer, 1) == 0) {
            ((char*)p3)[size] = 'Z'; 
            printf("INFO: Logical overflow was NOT detected (expected behavior for NoGuard).\n");
        } else {
            printf("SURPRISE: Logical overflow WAS detected!\n");
        }
    } // buffer destroyed here

    return 0;
}
