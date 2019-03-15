Philips Hue smart home application and Android phones and watches.

This app is built with native Java and utilizes the Philips Hue API as well as the Android wear API.

Published to the play store at:
https://play.google.com/store/apps/details?id=com.diplodesigns.huewatch


To start debugging the watch in Android Studio:

Open up Android Studio terminal ->

1. adb start-server

2. adb forward tcp:4444 localabstract:/adb-hub

3. adb connect localhost:4444
