# Release Signing

This project uses a **temporary development keystore** for local installation/testing.
Do not use this key for production distribution.

## Keystore files
- Real config: `keystore.properties` (not committed by default)
- Template: `keystore.properties.example`
- Dev key path: `.keystore/choosemeal-dev.jks`

## Build release
```powershell
cd d:\work\ChooseMeal
.\gradlew.bat assembleRelease
```

Output:
- `d:\work\ChooseMeal\app\build\outputs\apk\release\app-release.apk`
