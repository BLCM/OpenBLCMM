@echo off

Rem Original commandline, prior to pulling that config into the jarfile
Rem native-image -Djava.awt.headless=false -H:ReflectionConfigurationFiles=conf-dir/reflect-config.json -H:ResourceConfigurationFiles=conf-dir/resource-config.json -H:JNIConfigurationFiles=conf-dir/jni-config.json -jar OpenBLCMM.jar

echo Compiling using native-image
echo ----------------------------
echo.
pushd ..\store
call native-image -jar OpenBLCMM.jar
echo.

echo Setting icon on EXE
echo -------------------
echo.
RCEDIT64.exe /I OpenBLCMM.exe ..\windows-processing\openblcmm.ico
echo.

echo Setting to Windows Subsystem (avoids cmd.exe-window spawning)
echo -------------------------------------------------------------
echo.
Rem See https://github.com/oracle/graal/issues/2256
EDITBIN /SUBSYSTEM:WINDOWS OpenBLCMM.exe
echo.

echo Setting the app as High-DPI Capable
echo -----------------------------------
echo.
MT.exe -manifest ..\windows-processing\OpenBLCMM.exe.manifest -outputresource:OpenBLCMM.exe;#1
echo.

echo Collecting into subdir
echo ----------------------
echo.
rd compiled /s /q
md compiled
copy *.dll compiled
copy OpenBLCMM.exe compiled
echo.

echo Reporting
echo ---------
echo.
dir compiled
echo.

popd

