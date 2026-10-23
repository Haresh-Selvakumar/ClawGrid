# VS Code editor setup

Open `ClawGrid.code-workspace` with File > Open Workspace from File, or open the
whole project root folder. Opening only `main.cpp` does not load workspace settings.

`.vscode/c_cpp_properties.json` provides separate ESP A and ESP B configurations.
Select the matching configuration when working on either role. Each uses the
existing ESP32-S3 cross compiler, actual SDK include directories and generated
role-specific compile database. Included role source files also have explicit
entries so the editor knows they use native ESP-IDF rather than Arduino.

If old diagnostics remain, run C/C++: Reset IntelliSense Database from Ctrl+Shift+P.
After relocating the project or rebuilding into a different SDK, regenerate with
`python scripts/setup_vscode.py` using the pinned development Python environment.
This setup does not disable diagnostics, modify firmware, or upload boards.
