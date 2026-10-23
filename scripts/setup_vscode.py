"""Generate editor settings from the successful native builds; never changes firmware."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    output = ROOT / '.vscode'
    output.mkdir(exist_ok=True)
    configurations = []
    for role, environment, node in [('A', 'native_voice', 'node_voice'),
                                    ('B', 'native_intelligence', 'node_intelligence')]:
        build = ROOT / 'native/.pio/build' / environment
        rows = json.loads((build / 'compile_commands.json').read_text(encoding='utf-8'))
        entry = next(row for row in rows if Path(row['file']).name == 'app_entry.cpp')
        # CMake's export omits PlatformIO's role/native flags. Supply them for
        # editor analysis, including the role source included by app_entry.cpp.
        flags = f' -DCLAWGRID_NATIVE_IDF=1 -DCLAWGRID_NODE_{role}=1'
        for row in rows:
            row['command'] += flags
        direct = dict(entry)
        direct['file'] = (ROOT / f'firmware/{node}/src/main.cpp').as_posix()
        direct['command'] = entry['command'].rsplit(' -c ', 1)[0] + flags + ' -c "' + direct['file'] + '"'
        rows.append(direct)
        database = output / f'compile_commands.{role}.json'
        database.write_text(json.dumps(rows, indent=2) + '\n', encoding='utf-8')
        includes = []
        for path in re.findall(r'(?:^|\s)-I([^\s]+)', entry['command']):
            resolved = Path(path)
            if not resolved.is_absolute():
                resolved = Path(entry['directory']) / resolved
            resolved = resolved.resolve()
            if not resolved.is_dir():
                raise RuntimeError(f'Build include directory missing: {resolved}')
            try:
                include = '${workspaceFolder}/' + resolved.relative_to(ROOT).as_posix()
            except ValueError:
                include = resolved.as_posix()
            if include not in includes:
                includes.append(include)
        compiler = entry['command'].split('.exe', 1)[0] + '.exe'
        if not Path(compiler).is_file():
            raise RuntimeError('Native cross compiler is missing')
        configurations.append(dict(
            name=f'ESP {role} - {environment}',
            compilerPath=Path(compiler).as_posix(),
            compileCommands='${workspaceFolder}/.vscode/' + database.name,
            includePath=includes,
            defines=['CLAWGRID_NATIVE_IDF=1', f'CLAWGRID_NODE_{role}=1',
                     'HAVE_CONFIG_H', 'ESP_PLATFORM', '_GNU_SOURCE',
                     '_POSIX_READER_WRITER_LOCKS', 'IDF_VER="4.4.7"',
                     'MBEDTLS_CONFIG_FILE="mbedtls/esp_config.h"'],
            cStandard='gnu99', cppStandard='gnu++11'))
    (output / 'c_cpp_properties.json').write_text(
        json.dumps(dict(configurations=configurations, version=4), indent=2) + '\n', encoding='utf-8')
    (ROOT / 'ClawGrid.code-workspace').write_text(
        json.dumps(dict(folders=[dict(path='.')]), indent=2) + '\n', encoding='utf-8')
    print('Created two ESP editor configurations from verified build include paths.')
    print('Open the project folder or ClawGrid.code-workspace in VS Code.')


if __name__ == '__main__':
    main()
