# Issues with Java Install on Windows

Starting with JDK 11, the packaging of Java has changed significantly on Windows. This page gives some tips for install.

The most important change is that Oracle (and OpenJDK) no longer package a seperate JRE. There is only the JDK. This is probably a good thing, but it causes some difficulties with existing tools.

## Installing Oracle JDK

The Oracle JDK can be obtined from https://java.oracle.com. You will need to accept the license, then download and install the binary. Typically this installs in a directory like `C:\Program Files\Java\jdk-12.0.1`.

## Installing Open JDK
Open JDK builds can be obtained from https://adoptopenkdk.net.  When you install a JDK it will typically install in a directory like `C:\Program Files\AdoptOpenJDK\jdk-12.0.1.12-hotspot`.

## After Installing

Neither of these installs will add the Java executables to the PATH, and neither install will setup the JAVA_HOME environment variable. Again I think this is a good thing - but you will need to take some care after installing one of these JDK to make your tools work again.

## Setup your PATH
First, and most importantly, you will need to add the Java executables to your system PATH. You should do this through the control panel. My advice is to add you most up to date Java version to the system path. Edit the path variable and add the `bin` directory under your install to the PATH. For example `C:\Program Files\Java\jdk-12.0.1\bin` for Oracle JDK or `C:\Program Files\AdoptOpenJDK\jdk-12.0.1.12-hotspot\bin` for OpenJDK.

After you take this step, tools like Eclipse will be able to start.

## Setup JAVA_HOME
In general, I do not recommend setting JAVA_HOME in any kind of permanent fashion. JAVA_HOME is only needed for command line tools (like running Mavan from the command line). In the case that I ever need to do that, then I will set JAVA_HOME in the shell as needed.

To set JAVA_HOME in Powershell, enter a command like this:

```powershell
$Env:JAVA_HOME="C:\Program Files\AdoptOpenJDK\jdk-12.0.1.12-hotspot"
```

To set JAVA_HOME in a Windows command window, enter a command like this:

```shell
set JAVA_HOME=C:\Program Files\AdoptOpenJDK\jdk-12.0.1.12-hotspot
```

## Visual Studio Code Setup
Visual Studio Code should see where Java is installed when you have added the Java executables to the PATH. If it doesn't, or if you want to use a different JDK for compilation than the version you have in the path, you will need to change the `java.home` setting in VS Code. You can do this by...

1. Press ctrl+, to open the setting window
1. Search for `java.home`
1. Enter a value that is properly escaped. For example

   ```shell
   "java.home": "C:\\Program Files\\AdoptOpenJDK\\jdk-12.0.1.12-hotspot"
   ```

I recommend setting this value at the workspace level rather than the user level. This will give you some flexability in the future if you want different workspaces for different Java versions.


