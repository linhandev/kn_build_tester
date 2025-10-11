// Another sample header file to test the parser
#pragma once

#include <iostream>

// Macro definitions (should be ignored)
#define PI 3.14159
#define SQUARE(x) ((x) * (x))

/*
 * Multi-line comment
 * This should be ignored
 * void fakeFunction();
 */

// Simple functions
void start();
void stop();

// Function with namespace
namespace Math {
    double add(double a, double b);
    double subtract(double a, double b);
}

// Class with methods
class Calculator {
    private:
        double value;
    
    public:
        Calculator();
        ~Calculator();
        void clear();
        double getValue() const;
        void setValue(double val);
};

// Inline function (declaration only)
inline int square(int x);

#endif
