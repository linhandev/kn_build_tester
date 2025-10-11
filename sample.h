// Sample C++ header file for parsing demonstration
#ifndef SAMPLE_H
#define SAMPLE_H

#include <string>
#include <vector>

// Simple macro (will be ignored)
#define MAX_SIZE 100
#define MIN(a, b) ((a) < (b) ? (a) : (b))

// Simple function declarations
void initializeSystem();
int calculateSum(int a, int b);
double computeAverage(double* values, int count);

// Function with return type and parameters
std::string formatMessage(const std::string& message, int priority);
bool validateInput(const char* input);

// Function with default parameters
void setConfiguration(int timeout = 30, bool verbose = false);

// Function with pointer and reference parameters
void processData(int* data, size_t& size);
float* allocateBuffer(size_t elements);

// Const member function declaration (in class-like context)
class DataProcessor {
public:
    void process();
    int getCount() const;
    void setName(const std::string& name);
    virtual void update();
    static void reset();
};

// Template function (simple case)
template<typename T>
T getMaxValue(T a, T b);

// Namespace function
namespace Utils {
    void printDebugInfo();
    int convertToInt(const std::string& str);
}

// Function with multiple lines
std::vector<int> 
processArray(const std::vector<int>& input);

#endif // SAMPLE_H
