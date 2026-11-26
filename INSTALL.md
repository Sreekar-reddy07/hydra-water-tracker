# Install Hydra — step by step

## You need
- PC/Mac with Android Studio Ladybug+ (https://developer.android.com/studio)
- S23 Ultra + Galaxy Watch Ultra, both charged, Bluetooth on
- USB-C cable

## 1. Open the project
1. In Android Studio: File > Open > select `water-tracker-app` folder
2. Wait for Gradle sync (first time 2-5 min). If it asks, accept SDK 34 download.

## 2. Phone (S23 Ultra)
1. Phone: Settings > About phone > tap Build number 7x > Developer options ON > USB debugging ON
2. Plug phone into PC, tap Allow on phone
3. In Android Studio top bar, select `phone` module + your S23 Ultra device, press Run ▶
4. On first launch, allow:
   - Notifications
   - Alarms & reminders (Settings > Apps > Hydra > Alarms & reminders > Allow)
   - Do Not Disturb access (tap the prompt in app, or Settings > Apps > Special access > Do Not Disturb access > Hydra > Allow)
   - Battery > Unrestricted (Settings > Apps > Hydra > Battery > Unrestricted) — Samsung kills repeating alarms otherwise

## 3. Watch (Galaxy Watch Ultra)
1. Watch: Settings > About watch > tap Software version 7x > Developer options ON > Wireless debugging ON
2. On PC terminal: `adb connect <watch-IP>:5555` (IP shown on watch Wireless debugging screen)
   Or use Android Studio's Device Manager > Pair using Wi-Fi
3. In Android Studio, select `wear` module + your Watch Ultra, press Run ▶
4. On watch, allow Notifications for Hydra.

## 4. Set up the Quick Button (middle orange button) — do this once
1. On watch: Settings > Buttons and gestures > Action > select **Hydra**
   Or on phone: Galaxy Wearable app > Watch settings > Buttons and gestures > Quick button > Action > Hydra
2. Set "Start action with" to Short press.
3. Without this, the Quick Button will still do Flashlight / Workout — Samsung requires this assignment, Hydra can't override it on its own.

## 5. Test it (2 min)
1. On phone, drag the Daily goal slider to 2000 ml — watch should update to 2000 ml.
2. On phone, tap "Test vibrate + sound" — put phone AND watch on mute/vibrate/silent, both should buzz non-stop.
3. Press the middle Quick Button on the watch — buzzing should stop on BOTH phone and watch, and phone total should go up by 250 ml.
4. Test again, this time tap "Stop vibration" on the watch instead — buzzing stops but total does NOT go up.
5. On watch, tap "I Drank" — phone total should go up by 250 ml within 2 sec.

## Troubleshooting
- **No vibration on mute?** Vibration now uses direct Vibrator, not notification — it should buzz even on mute. If not, check Settings > Apps > Hydra > Battery > Unrestricted, and that VibrationService is running.
- **Quick Button doesn't stop it?** Check it is assigned to Hydra (step 4 above). If Hydra is open, it uses KEYCODE_STEM_1; if not open, it launches Hydra which stops vibration on launch.
- **No sound on mute?** Check DND access is Allowed + channel sound is set to Alarm. Re-run "Test vibrate + sound".
- **Reminder stops after a while?** Samsung battery optimization — set to Unrestricted, and keep "Alarms & reminders" allowed.
- **Watch doesn't sync?** Check Bluetooth is on, Wear OS app is connected, and both apps are same version. Re-open phone app once.
- **Gradle sync fails?** Check internet, then File > Invalidate Caches > Restart.

## Changing defaults
Daily goal is now a slider in the app (0–4000 ml, default 2000 ml). No code change needed. In `WaterRepository.kt`: `goal_ml` default 2000, `drink_ml` 250, `interval_min` 60.
