# weak: OH_HiTrace_IsTraceEnabled

## Results

| Binary | API 16 | API 21 |
|--------|--------|--------|
| main_weak | Loads; exception (weak stub) → EXIT=1 | Loads; exception (weak stub) → EXIT=1 (no NEEDED hitrace, resolves to weak) |
| main_weak_l | Loads; exception (weak stub) → EXIT=1 | Loads; exception (weak stub) → EXIT=1 (link-time bind to weak; device lib not used) |
| main_weak_zlazy | Loads; exception (weak stub) → EXIT=1 | Loads; exception (weak stub) → EXIT=1 |
| main_weak_znow | Loads; exception (weak stub) → EXIT=1 | Loads; exception (weak stub) → EXIT=1 |
| main_strong | Loads; exception (weak stub) → EXIT=1 | Loads; exception (weak stub) → EXIT=1 (this device: weak used) |
| main_call_then_dlopen | Loads; first call → exception → EXIT=1 | Loads; first call → exception → EXIT=1 (a.1: second call still weak on device with symbol) |
| main_dlopen_then_call | Loads; dlsym fails → exception → EXIT=1 | Loads; dlsym + call through pointer → trace_enabled=0 or 1, EXIT=0 (global from lib) |
