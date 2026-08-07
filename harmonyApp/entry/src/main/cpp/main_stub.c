/* Satisfy KN shared-lib UND main (__libc_start_main) for on-demand dlopen. */
int main(int argc, char **argv)
{
    (void)argc;
    (void)argv;
    return 0;
}
