#include <stdio.h>
#include <string.h>

// Include the generated header
#include "libapp_api.h"

int main() {
    printf("C Caller: Starting Kotlin demo...\n");
    fflush(stdout);
    
    // Get the library symbols - this initializes the Kotlin runtime
    libapp_ExportedSymbols* lib = libapp_symbols();
    
    // Call runAppDemo through the symbols table
    lib->kotlin.root.com.example.app.runAppDemo();
    
    // Also call runDemo which returns a string
    printf("\nC Caller: Calling runDemo() directly:\n");
    const char* result = lib->kotlin.root.com.example.app.runDemo();
    printf("%s\n", result);
    
    // Dispose the string when done
    lib->DisposeString(result);
    
    printf("C Caller: Demo finished.\n");
    fflush(stdout);
    return 0;
}
