@echo off
set OLDPATH=%CD%
set MAINPATH=%~dp0\..
cd %MAINPATH%
git-crypt unlock %USERPROFILE%/.git-crypt-key
cd %OLDPATH%
