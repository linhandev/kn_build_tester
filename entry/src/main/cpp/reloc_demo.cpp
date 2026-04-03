extern "C" void the_missing_symbol(void);

extern "C" __attribute__((visibility("default"))) void reloc_demo_export_triggers_unresolved_reloc(void)
{
    the_missing_symbol();
}
