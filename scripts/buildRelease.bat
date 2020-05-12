@echo off
set OLDPATH=%CD%
set MAINPATH=%~dp0\..
rem because javafx is not contained in every java version, i set this to my local path of bellsoft jdk. i know that this doesn't work for you.
set BELLSOFTPATH=D:\Programme\bellsoft-jdk-11.0.5
if EXIST %BELLSOFTPATH% (
	echo set JAVA_HOME to %BELLSOFTPATH%
	set JAVA_HOME=%BELLSOFTPATH%
)
cd %MAINPATH%
gradlew -Prelease.scope=patch clean build final
cd %OLDPATH%
