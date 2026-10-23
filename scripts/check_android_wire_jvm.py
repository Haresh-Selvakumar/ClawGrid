"""Run the app's actual Kotlin wire code/JUnit tests using cached official jars."""
import os
import shutil
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'.android-tools/gradle-cache/caches/modules-2/files-2.1'
JAVA=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Android\Android Studio\jbr'))/'bin/java.exe'


def jar(module,version):
    paths=list((CACHE/module/version).glob('*/*.jar'))
    if len(paths)!=1:raise RuntimeError('Missing cached JVM dependency: '+module+'/'+version)
    return str(paths[0])


def main():
    std=jar('org.jetbrains.kotlin/kotlin-stdlib','2.2.10')
    coroutine=jar('org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm','1.8.0')
    annotations=jar('org.jetbrains/annotations','23.0.0')
    compiler=[jar('org.jetbrains.kotlin/kotlin-compiler-embeddable','2.2.10'),std,
              jar('org.jetbrains.kotlin/kotlin-script-runtime','2.2.10'),
              jar('org.jetbrains.kotlin/kotlin-reflect','1.6.10'),coroutine,annotations]
    app=ROOT/'ecosystem/android/app'
    classes=ROOT/'.android-tools/jvm-wire'
    classes.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(app/'src/test/resources/realtime-vector.json',classes/'realtime-vector.json')
    dependencies=[std,annotations,coroutine,jar('com.squareup.okio/okio-jvm','3.4.0'),
                  jar('org.json/json','20240303'),jar('junit/junit','4.13.2'),
                  jar('org.hamcrest/hamcrest-core','1.3'),
                  str(app/'build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes')]
    sources=[app/'src/main/java/com/example/data/remote/RealtimeWire.kt',app/'src/test/java/com/example/RealtimeWireTest.kt']
    subprocess.run([str(JAVA),'-cp',os.pathsep.join(compiler),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                    '-no-stdlib','-no-reflect','-jvm-target','11','-classpath',os.pathsep.join(dependencies),
                    '-d',str(classes),*[str(p) for p in sources]],check=True,cwd=ROOT)
    subprocess.run([str(JAVA),'-cp',os.pathsep.join([str(classes),*dependencies]),'org.junit.runner.JUnitCore',
                    'com.example.RealtimeWireTest'],check=True,cwd=ROOT)


if __name__=='__main__':main()
