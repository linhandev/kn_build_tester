#include <iostream>
#include <sys/mman.h>
#include <unistd.h>
#include <stdexcept>
#include <cstdint>
#include <signal.h>
#include <setjmp.h>
#include <cstring>

enum class ProtectionType {
    Underflow,
    Overflow
};

#define PAGE_SIZE 4096 // 16KB for macOS ARM64. Standard Linux is often 4096.

class ProtectedBuffer {
private:
    void* actual_start = nullptr;
    size_t actual_size = 0;
    void* user_start = nullptr;

public:
    ProtectedBuffer(size_t size, ProtectionType type) {
        if (size == 0) return;

        // Calculate pages needed for data
        size_t data_pages = (size + PAGE_SIZE - 1) / PAGE_SIZE;
        
        // We need at least one guard page.
        // Total pages = data_pages + 1 (guard)
        size_t total_pages = data_pages + 1;
        actual_size = total_pages * PAGE_SIZE;

        // Use actual_start directly
        actual_start = mmap(NULL, actual_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (actual_start == MAP_FAILED) {
            throw std::runtime_error("mmap failed");
        }

        void* guard_page = nullptr;

        if (type == ProtectionType::Overflow) {
            // Layout: [ DATA ... ] [ GUARD ]
            // Guard is the LAST page(s).
            guard_page = (char*)actual_start + (data_pages * PAGE_SIZE);
            
            // User pointer is calculated such that it ends at guard_page
            user_start = (char*)guard_page - size;
            
        } else { // Underflow
            // Layout: [ GUARD ] [ DATA ... ]
            // Guard is the FIRST page.
            guard_page = actual_start;
            
            // User pointer is the start of the second page (or just after guard)
            user_start = (char*)actual_start + PAGE_SIZE;
        }

        // Protect the guard page
        if (mprotect(guard_page, PAGE_SIZE, PROT_NONE) == -1) {
            munmap(actual_start, actual_size);
            throw std::runtime_error("mprotect failed");
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

    return 0;
}
