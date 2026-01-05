// callee.cpp - Library code: struct definition hidden in .so
#include <iostream>

struct Data__ {
  int value = 2;
  const char* message = "default message";
};

typedef struct Data__* Data;

// Factory function to create Data using callee's struct definition
extern "C" Data create_data() {
  return new Data__;  // Uses callee's Data__ definition
}

// Strong implementation (overrides weak symbol from caller)
extern "C" void weak_function(Data data) {
  if (data) {
    std::cout << "Weak function actual implementation from callee\n";
    std::cout << "  Data value: " << data->value << "\n";
    std::cout << "  Data message: " << data->message << "\n";
  } else {
    std::cout << "Weak function actual implementation from callee (null data)\n";
  }
}
