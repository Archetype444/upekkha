#!/usr/bin/env python3
from pathlib import Path
import os,subprocess,sys
root=Path(__file__).resolve().parent
command=[str(root/'gradlew'),'assembleDebug','assembleRelease','--console=plain']
sys.exit(subprocess.call(command,cwd=root,env=os.environ.copy()))
