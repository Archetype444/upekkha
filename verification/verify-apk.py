from pathlib import Path
import os,subprocess,hashlib,json,zipfile,sys
import argparse
parser=argparse.ArgumentParser(description="Verify Upekkhā 3.0 APK identity, signature, alignment and media")
parser.add_argument("apk",type=Path)
parser.add_argument("--sdk",type=Path,default=os.environ.get("ANDROID_HOME",os.environ.get("ANDROID_SDK_ROOT")))
parser.add_argument("--java-home",type=Path,default=os.environ.get("JAVA_HOME"))
args=parser.parse_args()
root=Path(__file__).resolve().parent.parent
apk=args.apk.resolve()
if args.sdk is None:parser.error("Set ANDROID_HOME or pass --sdk")
bt=args.sdk/"build-tools/36.0.0"
env=os.environ.copy()
if args.java_home:
 env["JAVA_HOME"]=str(args.java_home);env["PATH"]=str(args.java_home/"bin")+os.pathsep+env["PATH"]
env["LD_LIBRARY_PATH"]=os.pathsep.join([str(bt/"lib64")]+([str(args.java_home/"lib"),str(args.java_home/"lib/server")] if args.java_home else [])+[env.get("LD_LIBRARY_PATH","")])
sig=subprocess.run([str(bt/'apksigner'),'verify','--verbose','--print-certs',str(apk)],env=env,capture_output=True,text=True,check=True).stdout
badging=subprocess.run([str(bt/'aapt'),'dump','badging',str(apk)],env=env,capture_output=True,text=True,check=True).stdout
manifest=subprocess.run([str(bt/'aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],env=env,capture_output=True,text=True,check=True).stdout
resources=subprocess.run([str(bt/'aapt'),'dump','resources',str(apk)],env=env,capture_output=True,text=True,check=True).stdout
cert='a5edb5474ed2435c35c086b1920a0acfdfeac91ad00c7fb6659c9017a44e2268'
assert cert in sig
assert "package: name='org.breatheless.app' versionCode='4' versionName='3.0'" in badging
assert "sdkVersion:'31'" in badging and "targetSdkVersion:'31'" in badging
assert 'E: activity-alias' in manifest and '"org.breatheless.app.MainActivity"' in manifest and '"com.wanderwildwood.nikka.MainActivity"' in manifest and '"android.intent.category.LAUNCHER"' in manifest
assert 'android.permission.INTERNET' not in manifest
assert 'org.breatheless.app.PracticeService' in manifest and 'org.breatheless.app.MorningReminder' in manifest
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 mp3=[f for f in z.infolist() if f.filename.endswith('.mp3')];assert len(mp3)==1
 f=mp3[0];assert f.filename=='assets/audio/mindfulness.mp3' and f.compress_type==zipfile.ZIP_STORED
 audio=hashlib.sha256(z.read(f)).hexdigest();assert audio=='b6468bc0e09e70e2ba9723b463c748947b96c6f8808111a769d11d6cc44a178a'
 assert not any(n.endswith(('.p12','.jks','.keystore')) or n.endswith('password.txt') for n in z.namelist())
aligned=subprocess.run([str(bt/'zipalign'),'-c','-p','4',str(apk)],env=env,capture_output=True,text=True)
assert aligned.returncode==0,aligned.stdout+aligned.stderr
with zipfile.ZipFile(apk) as z:
 audioHashes={hashlib.sha256(z.read(f)).hexdigest() for f in z.infolist() if f.filename.endswith(('.wav','.ogg'))}
 originals=list((root/'app/src/main/res/raw').glob('*.wav'))+list((root/'app/src/main/res/raw').glob('*.ogg'))
 assert all(hashlib.sha256(p.read_bytes()).hexdigest() in audioHashes for p in originals)
for name in ['low_knock','two_low','rise','fall','bell','glass','wood','three_rise','three_fall','deep_bell','pulse','gong','chime','fifth','single','end','soft_double','tuning','alternating']:assert 'raw/cue_'+name in resources,name
for name in ['wind','fireplace','bowl','bowl_end','breath_in','breath_hold','breath_out']:assert 'raw/'+name in resources,name
report={'file':apk.name,'size':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'certificateSha256':cert,'applicationId':'org.breatheless.app','versionCode':4,'versionName':'3.0','minSdk':31,'targetSdk':31,'launcher':'org.breatheless.app.MainActivity','signatureVerified':True,'zipIntegrityVerified':True,'zipAlignmentVerified':True,'originalRawAudioFilesVerified':len(originals),'internetPermission':False,'allCueAndAmbienceResourcesPresent':True,'recordingCopies':1,'recordingStoredUncompressed':True,'recordingSha256':audio,'privateSigningMaterialPackaged':False}
apk.with_suffix('.verification.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
