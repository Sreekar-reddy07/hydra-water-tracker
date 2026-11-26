# Hydra — Water Tracker for S23 Ultra + Galaxy Watch Ultra

Built for Sreekar. Phone + Watch app that nags you until you drink.

## What's new in v2
1. **Daily goal slider** — on the phone home screen, slide 0–4000 ml in 100 ml steps. Example: 2000 ml. Presets: 500 / 1000 / 1500 / 2000 / 2500 / 3000 / 3500 / 4000. Default is now 2000 ml. Changing it syncs instantly to the watch.
2. **Continuous vibration, any sound mode** — reminder now vibrates non-stop on BOTH phone and watch, whether the phone/watch is on mute, vibrate, or full sound. It uses a direct `Vibrator` foreground service (not a notification buzz), so ringer mode can't silence it. It only stops when you press a button. Safety auto-stop: 30 min on phone, 10 min on watch.
3. **Quick Button (middle orange button) stops vibration** — Galaxy Watch Ultra's middle Quick Button now stops the vibration AND logs 250 ml. See setup below — you must assign it once in Settings.

## What it does
- **Daily goal:** slider, 0–4000 ml, default 2000 ml
- **Log a drink:** +250 ml default, also 100 / 500 ml buttons. Every tap is logged with time.
- **Loud reminder even on mute/vibrate:** Alarm channel (`USAGE_ALARM`, bypass DND) + full-screen intent + continuous vibration service. You must grant "Alarms & reminders" + "Do Not Disturb access" on first run.
- **Repeats until you drink:** Reminder fires every 60 min from 08:00–22:00. If you don't tap "I Drank" or "Snooze 5 min", it re-fires every 5 min. Vibration keeps going until you stop it.
- **Watch:** Big "I Drank 💧" button, progress ring, +100 / +500, plus "Stop vibration" button (stops buzz without logging). Quick Button = I Drank.
- **History:** Phone shows today, total ml, recent drinks.

## Quick Button setup — do this once (required)
Samsung doesn't let an app hijack the Quick Button globally. You assign it once, then Hydra handles the press:

**On the watch:**
1. Watch Settings > Buttons and gestures > Action
2. Scroll to Features / Apps > select **Hydra**
3. Back > Start action with > Short press

**Or in the Galaxy Wearable app on your phone:**
1. Wearable app > Watch settings > Buttons and gestures > Quick button > Action > Hydra
2. Start action with > Short press

**How it works after that:**
- If Hydra is open when the reminder fires, pressing the Quick Button is caught as `KEYCODE_STEM_1` in `WatchActivity.onKeyDown()` — stops vibration on both devices + logs 250 ml.
- If Hydra is NOT open, pressing it launches Hydra, and `onCreate()` stops the vibration immediately — same result.

If you leave it set to Flashlight / Workout, pressing it won't stop Hydra — that's a Samsung limitation, not a bug.

## Project layout
```
Hydra/
  phone/   — S23 Ultra app (Jetpack Compose, Room, AlarmManager, VibrationService)
  wear/    — Galaxy Watch Ultra app (Wear Compose, Tile, WatchVibrationService)
  shared/  — shared models + Data Layer paths
```

## How to run it (10 min)
1. Install **Android Studio Ladybug or newer** on your PC/Mac
2. Open this folder in Android Studio → let Gradle sync
3. Connect S23 Ultra via USB, enable Developer Options > USB Debugging
4. Run `phone` module → installs on phone
5. Connect Watch Ultra via Wi-Fi debugging, then Run `wear` module → installs on watch
6. On phone: allow Notifications, Alarms & Reminders, DND access when prompted. On watch: allow Notifications.
7. Do the Quick Button setup above.
8. Test: tap "Test vibrate + sound" on phone — put phone AND watch on mute, both should buzz non-stop until you press the watch Quick Button or tap I Drank.

## Permissions needed
- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM` + `USE_FULL_SCREEN_INTENT`
- `ACCESS_NOTIFICATION_POLICY` (DND bypass)
- `FOREGROUND_SERVICE` + `VIBRATE`
- On Samsung: Settings > Apps > Hydra > Battery > Unrestricted

## Files to know
- `phone/.../ui/MainActivity.kt` — goal slider (0–4000 ml)
- `phone/.../reminder/VibrationService.kt` — continuous phone vibration
- `phone/.../reminder/ReminderManager.kt` — schedules alarms, starts vibration on phone + watch
- `wear/.../sync/WatchVibrationService.kt` — continuous watch vibration
- `wear/.../ui/WatchActivity.kt` — Quick Button `onKeyDown` for `KEYCODE_STEM_1/2/3`
- `wear/.../ui/WatchMain.kt` — big Drink button + Stop vibration button
- `shared/.../Sync.kt` — paths `/hydra/drink`, `/hydra/progress`, `/hydra/start_vibrate`, `/hydra/stop_vibrate`, `/hydra/goal`
