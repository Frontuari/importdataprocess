# Import Data Process - Target Platform

A target platform is necessary to build a iDempiere plugin. This is a smart target platform.


## Prerequisites

- Java 11, commands `java` and `javac`.
- Maven 3.6.0
- Set `IDEMPIERE_REPOSITORY` env variable.

## Getting started

Compile target platform:

```bash
./plugin-builder
```

You could create a file `plugins.txt` with all plugin's paths on newlines, example:

```
/plugin-path-1
/plugin-path-2
```

Compile target platform and plugins:

```bash
./plugin-builder /plugin-path-1 /plugin-path-2
```

Use the parameter `debug` for debug mode example:

```bash
./plugin-builder debug /plugin-path-1 /plugin-path-2
```

To use `.\plugin-builder.bat` for windows.

## How it works

### Update the `pom.xml` file

Go to the `./pom.xml` file and add the plugin's relative path, example `net.frontuari.importdataprocess`:

```xml
    <modules>
        <module>net.frontuari.importdataprocess.p2.targetplatform</module>
        <module>../net.frontuari.importdataprocess</module>
    </modules>
```

### Add the iDempiere's absolute path in p2.targetplatform

Go to `net.frontuari.importdataprocess.p2.targetplatform/net.frontuari.importdataprocess.p2.targetplatform.target` and update the path:

```xml
    <repository location="file:///home/user/idempiere/org.idempiere.p2/target/repository"/>
```

### Update the plugin's relative path to iDempiere

Go to `net.frontuari.importdataprocess.p2.targetplatform/pom.xml` and update:

```xml
    <relativePath>../../idempiere/org.idempiere.parent/pom.xml</relativePath>
```
