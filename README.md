To start debugging the watch in Android Studio:

Open up Android Studio terminal ->

1. adb start-server

2. adb forward tcp:4444 localabstract:/adb-hub

3. adb connect localhost:4444