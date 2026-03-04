# Build And Run

## Prerequisites
- Windows + JDK (already available on this machine)
- Android SDK installed at `d:\work\ChooseMeal\.tooling\android-sdk`

## Commands
```powershell
cd d:\work\ChooseMeal
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
```

## Install APK
Debug APK:
- `app\build\outputs\apk\debug\app-debug.apk`

Release APK:
- `app\build\outputs\apk\release\app-release.apk`

Install with adb:
```powershell
d:\work\ChooseMeal\.tooling\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\release\app-release.apk
```
