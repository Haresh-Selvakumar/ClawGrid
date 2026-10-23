"""Host-run the actual Android control adapter and JUnit cases; no Android or boards required."""
import os
import subprocess
from pathlib import Path
from check_android_wire_jvm import ROOT, JAVA, jar


def main():
    std = jar('org.jetbrains.kotlin/kotlin-stdlib', '2.2.10')
    coroutine = jar('org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm', '1.8.0')
    annotations = jar('org.jetbrains/annotations', '23.0.0')
    compiler = [jar('org.jetbrains.kotlin/kotlin-compiler-embeddable', '2.2.10'), std,
                jar('org.jetbrains.kotlin/kotlin-script-runtime', '2.2.10'),
                jar('org.jetbrains.kotlin/kotlin-reflect', '1.6.10'), coroutine, annotations]
    dependencies = [std, jar('org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm', '1.10.2'), annotations, jar('org.json/json', '20240303'),
                    jar('com.squareup.okhttp3/okhttp', '4.10.0'), jar('com.squareup.okio/okio-jvm', '3.4.0'),
                    jar('junit/junit', '4.13.2'), jar('org.hamcrest/hamcrest-core', '1.3')]
    app = ROOT / 'ecosystem/android/app'
    classes = ROOT / '.android-tools/jvm-control'
    classes.mkdir(parents=True, exist_ok=True)
    sources = [app / 'src/main/java/com/example/data/remote/HardwareRepository.kt',
               app / 'src/main/java/com/example/data/remote/LocalVoiceRepository.kt',
               app / 'src/main/java/com/example/data/remote/GatewayAddress.kt',
               app / 'src/test/java/com/example/HardwareRepositoryTest.kt',
               app / 'src/test/java/com/example/LocalVoiceTest.kt',
               app / 'src/test/java/com/example/HttpBoardApiTest.kt']
    subprocess.run([str(JAVA), '-cp', os.pathsep.join(compiler), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                    '-no-stdlib', '-no-reflect', '-jvm-target', '11', '-classpath', os.pathsep.join(dependencies),
                    '-d', str(classes), *map(str, sources)], check=True, cwd=ROOT)
    subprocess.run([str(JAVA), '-cp', os.pathsep.join([str(classes), *dependencies]),
                    'org.junit.runner.JUnitCore', 'com.example.HardwareRepositoryTest',
                    'com.example.HttpBoardApiTest', 'com.example.LocalVoiceTest'], check=True, cwd=ROOT)


if __name__ == '__main__':
    main()
