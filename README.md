# A1 Osmosmerka Bot prototype

Android prototype calibrated to the supplied 691×1536 screenshot. It captures the screen, classifies the 7×7 board using five templates cropped from that screenshot, searches every adjacent swap, and uses Accessibility gestures for the highest-clearing legal move.

## Build
Open this folder in Android Studio (JDK 17), let Gradle sync, then Build > Build APK(s). Minimum Android 10 (API 29), target API 35.

## Run
1. Install/open A1 Osmosmerka Bot.
2. Tap **ALLOW SCREEN CAPTURE** and approve Android's capture dialog.
3. Tap **ENABLE ACCESSIBILITY BOT**, select **A1 Osmosmerka Bot**, and enable it.
4. Return to the bot and tap **START BOT**.
5. Switch to the A1 game with the 7×7 board visible.
6. To stop, return to the bot and press **STOP BOT**, or disable its Accessibility service.

## Important
This is calibrated to the screenshot supplied in chat. If the A1 app/browser changes scaling, status/navigation bars, or board position, edit `center()` in `BotAccessibilityService.kt`.

The five template PNGs in `res/drawable` came from the supplied screenshot. Classification is intentionally dependency-free and fast rather than sophisticated.
