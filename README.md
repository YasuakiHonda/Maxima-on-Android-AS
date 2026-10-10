Source code distribution of Maxima on Android
written by Yasuaki Honda (yasuaki.honda@gmail.com)

Copyright 2012,2013,2014,2015,2016,2017,2018,2019 Yasuaki Honda, Chiba, Japan

Building
- Android Studio / Gradle (the wrapper is included): ./gradlew assembleDebug
- The app runs Maxima 5.50.0 (ECL 26.5.5), gnuplot and QEPCAD B as native
  programs for arm64-v8a and x86_64, and shows the Maxima 5.50.0 manual and
  MathJax-rendered output in web views. None of these are committed: they are
  built from pinned upstream sources by the scripts in native/ (see
  native/README.md) and published as a GitHub Release; the Gradle build
  downloads the release named in native/prebuilt.properties. To use a local
  build instead, run native/docker-build.sh all and build with
  -Pmoa.nativeDir=native/out.

Testing
- ./gradlew testDebugUnitTest   JVM unit tests (app/src/test)
- ./gradlew lintDebug           Android Lint; any error fails the build
- ./gradlew check               both of the above
- ./gradlew connectedDebugAndroidTest
                                instrumented tests (app/src/androidTest) on a
                                connected device/emulator

This repository is provided in the hope that anyone can easily start the development of 
Maxima on Android using Google's Android Studio.

License
You can find all the license terms in the assets-About_MoA-index.html file. Here is a
copy of the same language:

Maxima on Android
Copyright 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 Yasuaki Honda (yasuaki.honda@gmail.com)
    MaximaOnAndroid is a combination of many open source software. 
    All such open source software are governed by their own license terms.
    They appear in other sections of this document. 
    The following license is applicable to the Java and HTML source codes 
    solely written by the author of MaximaOnAndroid.
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see http://www.gnu.org/licenses/.
