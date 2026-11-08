#!/usr/bin/env python3
"""Build and verify Upekkhā 2.1. The signing key is mandatory and never regenerated."""
from pathlib import Path
import os,subprocess,shutil,zipfile,hashlib,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
BUILD=ROOT/'build';BUILD.mkdir(exist_ok=True);(BUILD/'tmp').mkdir(exist_ok=True)
SDK=Path(os.environ.get('ANDROID_HOME',ROOT.parent/'tools/sdk'))
KOTLIN=Path(os.environ.get('KOTLIN_HOME',ROOT.parent/'tools/kotlinc'))
BT=Path(os.environ.get('BUILD_TOOLS',SDK/'build-tools/35.0.0' if (SDK/'build-tools/35.0.0').exists() else SDK/'android-15'))
PLATFORM=Path(os.environ.get('ANDROID_JAR',SDK/'platforms/android-35/android.jar' if (SDK/'platforms/android-35/android.jar').exists() else SDK/'android-35/android.jar'))
R8=Path(os.environ.get('R8_JAR',ROOT.parent/'tools/r8-8.9.35.jar'))
assert hashlib.sha256(R8.read_bytes()).hexdigest()=='204b2fc2b0f4e888dc0ef748b58090def1bf4185068d36abbb94841dbc7107a8','D8 checksum mismatch'
KEY=ROOT/'signing/breatheless-update.p12';PASS=ROOT/'signing/password.txt'
assert KEY.exists() and PASS.exists(),'Missing original v2 signing identity. Restore signing backup; do not generate a new key.'
identity=json.loads((ROOT/'signing/identity.json').read_text());assert identity['applicationId']=='org.breatheless.app'
ENV=os.environ.copy();ENV['LD_LIBRARY_PATH']=os.pathsep.join([str(BT/'lib64'),'/usr/lib/jvm/java-17-openjdk-amd64/lib','/usr/lib/jvm/java-17-openjdk-amd64/lib/server',ENV.get('LD_LIBRARY_PATH','')]);ENV.pop('JAVA_TOOL_OPTIONS',None)
def run(*args):
 print('+',' '.join(map(str,args)),flush=True);subprocess.run(list(map(str,args)),cwd=ROOT,env=ENV,check=True)
JAVA=['java','-Xmx1024m','-XX:-UseContainerSupport','-XX:ActiveProcessorCount=2',f'-Djava.io.tmpdir={BUILD}/tmp']
def java(cp,main,*args):run(*JAVA,'-cp',cp,main,*args)
java(str(KOTLIN/'lib/*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',str(PLATFORM)+os.pathsep+str(KOTLIN/'lib/kotlin-stdlib.jar'),'-jvm-target','1.8','-d',BUILD/'app.jar',*sorted((ROOT/'app/src/main/kotlin').rglob('*.kt')))
run(BT/'aapt2','compile','--dir',ROOT/'app/src/main/res','-o',BUILD/'resources.zip')
ET.register_namespace('android','http://schemas.android.com/apk/res/android');m=ET.parse(ROOT/'app/src/main/AndroidManifest.xml');m.getroot().set('package','org.breatheless.app');m.write(BUILD/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
run(BT/'aapt2','link','-o',BUILD/'resources.apk','-I',PLATFORM,'--manifest',BUILD/'AndroidManifest.xml','--min-sdk-version','26','--target-sdk-version','35','--version-code','3','--version-name','2.1','-0','mp3','-A',ROOT/'app/src/main/assets',BUILD/'resources.zip')
(BUILD/'dex').mkdir(exist_ok=True)
java(str(R8),'com.android.tools.r8.D8','--debug','--min-api','26','--lib',PLATFORM,'--output',BUILD/'dex',BUILD/'app.jar',KOTLIN/'lib/kotlin-stdlib.jar',KOTLIN/'lib/annotations-13.0.jar')
shutil.copyfile(BUILD/'resources.apk',BUILD/'unsigned.apk')
with zipfile.ZipFile(BUILD/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in sorted((BUILD/'dex').glob('*.dex')):z.write(f,f.name)
run(BT/'zipalign','-f','4',BUILD/'unsigned.apk',BUILD/'aligned.apk')
APK=BUILD/'Upekkha-v2.1-debug.apk'
java(str(BT/'lib/apksigner.jar'),'com.android.apksigner.ApkSignerTool','sign','--ks',KEY,'--ks-key-alias','breatheless-update','--ks-pass',f'file:{PASS}','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',APK,BUILD/'aligned.apk')
verification=subprocess.check_output(list(map(str,JAVA+['-cp',str(BT/'lib/apksigner.jar'),'com.android.apksigner.ApkSignerTool','verify','--verbose','--print-certs',str(APK)])),cwd=ROOT,env=ENV,text=True)
print(verification,flush=True)
assert identity['certificateSha256'] in verification, 'Signing identity changed; this APK cannot update the installed v2 series'
run(BT/'zipalign','-c','-v','4',APK)
print('BUILT AND VERIFIED:',APK,flush=True)
