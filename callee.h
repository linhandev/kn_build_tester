// callee.h - Library header (caller doesn't have access to this)
// This mimics the napi_critical_scope pattern

// Forward declaration of opaque struct
struct Data__;

// Opaque pointer typedef - this is all the caller would see in a real header
typedef struct Data__* Data;

// Function declaration
extern "C" void weak_function(Data data);

