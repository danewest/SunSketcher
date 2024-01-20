# SunSketcher
NASA-funded Android app to crowdsource image data of Baily's Beads of 4/8/2024 total solar eclipse.

# Development Software Requirements
Android Studio Flamingo 2022.2.1 Patch 2, using Gradle 8.0.4

# Deployment Hardware Requirements
Android 12 or greater

# Troubleshooting
If the project throws an error when you try to build it saying that it's missing an SDK and does not prompt to change the SDK path, open the local.properties file in the project root (or under Gradle Scripts in Android Studio) and change `Akatsuki` on the last line `sdk.dir=C\:\\Users\\Akatsuki\\AppData\\Local\\Android\\Sdk` to whatever user folder your Android Studio installation uses.
