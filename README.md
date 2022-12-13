# ARTester (Android Runtime Tester)

<div align="center"><a href="https://github.com/mkenney/software-guides/blob/master/STABILITY-BADGES.md#mature"><img src="https://img.shields.io/badge/stability-mature-008000.svg" alt="Mature"></a><br/><br/><br/></div>

There are currently several apps available for Android devices that will allow you to compile and run code written in Java. But none of those other apps will let you do something that ARTester will.<br/>
ARTester will give you the opportunity to execute Java code from which you need to access Android API's that require a valid instance of the `android.content.Context` class, since ARTester will provide you with access (from your project) to the Activity that starts after compiling and running your project.

ARTester is intended to be used by those application developers, who for economic reasons do not have a desktop device among their belongings, and have had to manage to develop their apps only from an Android device, using some IDE available for Android devices.<br/>
If that is your case, you will know that if you plan to start developing complex functionality for any of the apps you are working on, your IDE will have to build an APK file that you will then have to install each time you want to test your app (to know how behaves with every important step you take). This is because absolutely all the IDE's available for Android devices do not offer functionality similar to what ARTester does.<br/>
With the above in mind, ARTester could help you reduce the time it takes to test each change, by not requiring you to install an update for your app for each test you want to perform. To achieve this goal, you can do the following:
1. Create a new empty project from ARTester
2. Then add to said project all the code related to the new functionality that you are developing for one of your apps
3. Make changes to the code, and run it through ARTester as many times as you want to test changes.
4. By the time you're done testing, and your new feature is ready to go live. Move all the code into your main project (from which your IDE will build an APK file).

The development of this app was possible thanks to [CodeAssist][CA], because that's the only IDE available for Android devices that doesn't have problems compiling some big libraries like **Javac** and **R8** (on which depends the functionality provided by ARTester).

## Features of ARTester that differentiate it from the other Java compilers available for Android devices
* Not intended to be an IDE, but intended to be used in conjunction with a future version of [CodeAssist][CA], which would allow you to disable storage restrictions that were added starting with Android 11.
* Does not integrate a text / code editor. So for the time being you will have to use some other app intended to accomplish that purpose, but you will need to choose an app that allows you to modify files in any directory within the internal storage shared volume of your device (even if your device runs Android 11 or later). I personally use [QuickEdit Pro](https://play.google.com/store/apps/details?id=com.rhmsoft.edit.pro), which offers the following advantages: high performance even when running on phones with less than 2GB of RAM, a good number of features, no bugs of any kind, and text scrolling is not diagonal. But unfortunately QuickEdit Pro does not have code completion.
* Does not integrate a file browser. Because any text editor that includes syntax highlighting also includes a file browser.
* Will only compile code written in Java (no Kotlin or XML)
* Will not generate APK files. Instead the projects will load and run immediately after the build process completes {SomeFile.java >> SomeFile.class >> SomeFile.dex >> `loadClass(SomeFile)`}.
* It does not support the download of external libraries (although it does support the inclusion of *.jar files previously downloaded from other apps and copied to a directory whose file path is included in the "config.txt" file belonging to the project in question).

## First steps
1. Download and install the [**ARTester.apk**](https://github.com/eliasoft/ARTester/raw/main/ARTester.apk) file that you will find in the root directory of this repository
2. Open the app and do the initial setup
3. Then click on the button **Create new example project**, and finally follow the instructions that will be indicated there.

##### If you want to be notified every time a new version of ARTester is released, perform the following steps:
 1. Sign in or sign up for Github, and then return to this page.
 2. Click the "Watch" button, which is located in the header of this repository.
 3. In the displayed pop-up menu, select the "Custom" option.
 4. Check the "Releases" check-box, and finally click the "Apply" button.
 
## Screenshots
<div align="center">
<img width="45%" src="https://github.com/eliasoft/ARTester/raw/main/Screenshots/Screenshot_2022-10-09-11-08-08-165_eliasoft.artester.jpg"/>
<img width="45%" src="https://github.com/eliasoft/ARTester/raw/main/Screenshots/Screenshot_2022-10-09-11-08-11-762_eliasoft.artester.jpg"/>
</div>

## Building - [CodeAssist][CA]

Download this repository to your local Android device and then open it on CodeAssist.

## Contributing

- Pull request must have a short description as a title and a more detailed one in the description

# Special thanks

- CodeAssist Javac port for Android

---

[CA]: https://github.com/tyron12233/CodeAssist
