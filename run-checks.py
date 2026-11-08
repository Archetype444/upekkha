#!/usr/bin/env python3
"""Run the production-core checks after building app.jar."""
from pathlib import Path
import os,subprocess
root=Path(__file__).resolve().parent
sdk=Path(os.environ.get('ANDROID_HOME',root.parent/'tools/sdk'));kotlin=Path(os.environ.get('KOTLIN_HOME',root.parent/'tools/kotlinc'))
platform=Path(os.environ.get('ANDROID_JAR',sdk/'platforms/android-35/android.jar' if (sdk/'platforms/android-35/android.jar').exists() else sdk/'android-35/android.jar'))
jsonjar=Path(os.environ.get('JSON_JAR',root.parent/'tools/json.jar'))
build=root/'build';assert (build/'app.jar').exists(),'Run build-apk.py first';assert jsonjar.exists(),'Provide org.json:json:20250517 using JSON_JAR'
env=os.environ.copy();env['LD_LIBRARY_PATH']=os.pathsep.join(['/usr/lib/jvm/java-17-openjdk-amd64/lib','/usr/lib/jvm/java-17-openjdk-amd64/lib/server',env.get('LD_LIBRARY_PATH','')]);env.pop('JAVA_TOOL_OPTIONS',None)
java=['java','-Xmx768m','-XX:-UseContainerSupport','-XX:ActiveProcessorCount=2',f'-Djava.io.tmpdir={build}/tmp']
cp=os.pathsep.join(map(str,[build/'app.jar',jsonjar,kotlin/'lib/kotlin-stdlib.jar',platform]))
subprocess.run(java+['-cp',str(kotlin/'lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-jvm-target','1.8','-d',str(build/'checks.jar'),str(root/'tests/Checks.kt')],env=env,check=True)
subprocess.run(java+['-cp',cp+os.pathsep+str(build/'checks.jar'),'org.breatheless.app.ChecksKt'],env=env,check=True)
