# GameFocus Release Checklist

## Pre-Build Verification
- [ ] google-services.json placed in app/ directory
- [ ] Firebase project configured with Firestore enabled
- [ ] Version code: 1, version name: "1.0.0"
- [ ] Application ID: com.gamefocus

## AndroidManifest Permissions
- [ ] android.permission.QUERY_ALL_PACKAGES (game detection)
- [ ] android.permission.FOREGROUND_SERVICE (foreground service)
- [ ] android.permission.ACCESS_NOTIFICATION_POLICY (DND)
- [ ] android.permission.SYSTEM_ALERT_WINDOW (optional overlay)

## Build Variants
- [ ] Debug build compiles and runs
- [ ] Release build compiles (proguard rules if any)
- [ ] APK generated successfully

## Testing (Manual)
- [ ] First launch shows onboarding
- [ ] Usage permission prompt appears
- [ ] Game detection works
- [ ] Session tracking works
- [ ] Focus mode toggles correctly
- [ ] Firebase sync works (when configured)
- [ ] Battery optimization settings accessible

## Assets
- [ ] App icon (ic_launcher_foreground/round)
- [ ] Splash screen theme

## Known Limitations
- Firebase sync requires google-services.json setup
- DND permission must be granted in system settings
- Some devices may kill background service aggressively

## APK Generation
```bash
./gradlew assembleRelease
```
Output: app/build/outputs/apk/release/GameFocus-v1.0.0.apk

## AAB Generation
```bash
./gradlew bundleRelease
```
Output: app/build/outputs/bundle/release/GameFocus-v1.0.0.aab

## GitHub Release
1. Create tag: v1.0.0
2. Upload APK and AAB to release
3. Include changelog