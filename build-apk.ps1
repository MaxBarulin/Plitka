# Локальная сборка APK без Android Studio.
# Тулчейн (JDK 21, Gradle 9.3.1, Android SDK) лежит в F:\android-build
$env:JAVA_HOME = "F:\android-build\jdk\jdk-21.0.12.1+1"
$env:ANDROID_HOME = "F:\android-build\sdk"
$env:ANDROID_SDK_ROOT = "F:\android-build\sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& "F:\android-build\gradle\gradle-9.3.1\bin\gradle.bat" @args
