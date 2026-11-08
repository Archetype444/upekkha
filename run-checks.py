#!/usr/bin/env python3
from pathlib import Path
import os,subprocess,sys
root=Path(__file__).resolve().parent
sys.exit(subprocess.call([str(root/'gradlew'),'testDebugUnitTest','lintDebug','--console=plain'],cwd=root,env=os.environ.copy()))
