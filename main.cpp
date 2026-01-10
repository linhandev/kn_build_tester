#include <iostream>
#include <cstdlib>

// Function to demonstrate function call coverage
int calculate(int a, int b, char op) {
    switch (op) {
        case '+':
            return a + b;
        case '-':
            return a - b;
        case '*':
            return a * b;
        case '/':
            if (b != 0) {
                return a / b;
            } else {
                return 0; // Division by zero
            }
        default:
            return -1; // Invalid operator
    }
}

int main(int argc, char* argv[]) {
    int x = 5;
    int y = 3;
    
    // If-else coverage demo
    if (argc > 1) {
        x = std::atoi(argv[1]);
    } else {
        x = 10;
    }
    
    if (argc > 2) {
        y = std::atoi(argv[2]);
    } else {
        y = 5;
    }
    
    // Test different operations
    int result1 = calculate(x, y, '+');
    int result2 = calculate(x, y, '-');
    int result3 = calculate(x, y, '*');
    int result4 = calculate(x, y, '/');
    int result5 = calculate(x, y, '%'); // Invalid operator
    
    std::cout << "Results: " << result1 << " " << result2 << " " 
              << result3 << " " << result4 << " " << result5 << std::endl;
    
    return 0;
}
