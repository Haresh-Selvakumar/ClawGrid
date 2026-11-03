#pragma once
#include <string.h>
// USB diagnostics forwards the SAME explicit room query as the phone. Reject
// unknown/duplicate parameters rather than silently reporting a different room.
inline bool bridge_path_valid(const char *path) {
    if(!path || strncmp(path,"/api/",5)) return false;
    const char *p=path;
    while(*p && *p!='?') {
        if(!((*p>='a' && *p<='z') || *p=='/' || *p=='_')) return false;
        ++p;
    }
    return !*p || !strcmp(p,"?room=1") || !strcmp(p,"?room=2");
}
