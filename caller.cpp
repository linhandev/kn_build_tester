// caller.cpp - Weak function with opaque pointer (typedef struct X__* X)
#include <iostream>

struct Data__;
typedef struct Data__* Data;

// Weak factory function (uses callee's if linked)
extern "C" __attribute__((weak)) Data create_data() {
  return nullptr;  // Default: can't create without callee's struct definition
}

// Default weak implementation (used if callee not linked)
extern "C" __attribute__((weak)) void weak_function(Data data) {
  std::cout << "Weak function default implementation (callee not linked)\n";
  if (data) {
    std::cout << "  (opaque pointer provided, but can't access members)\n";
  } else {
    std::cout << "  (null data)\n";
  }
}

void strong_function() {
  std::cout << "Strong function from caller\n";
}

int main() {
  std::cout << "=== Caller Program ===\n";
  strong_function();
  
  // Create Data using callee's implementation if linked, otherwise nullptr
  Data data = nullptr;
  if (create_data) {
    data = create_data();  // Uses callee's Data__ definition
  }
  std::cout << "Calling weak function, data from create_data :\n";
  weak_function(data);

  std::cout << "Calling weak function, data from constructor :\n";
  weak_function(Data());

  return 0;
}
