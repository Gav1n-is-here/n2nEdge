/* Preserve UTF-8 shared keys across the Windows UTF-16 environment boundary. */
#define main n2n_edge_main
#include "vendor/n2n/src/edge.c"
#undef main
int main(int argc, char **argv) {
    WCHAR wide[256];
    char key[1024];
    DWORD count = GetEnvironmentVariableW(L"N2N_KEY", wide, 256);
    if(count > 0 && count < 256) {
        if(!WideCharToMultiByte(CP_UTF8, 0, wide, -1, key, sizeof(key), NULL, NULL)) return 2;
        _putenv_s("N2N_KEY", key);
        SecureZeroMemory(wide, sizeof(wide));
        SecureZeroMemory(key, sizeof(key));
    }
    setvbuf(stdout, NULL, _IONBF, 0);
    return n2n_edge_main(argc, argv);
}
