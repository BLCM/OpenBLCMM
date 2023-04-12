@echo off

Rem Original commandline, prior to pulling that config into the jarfile
Rem native-image -Djava.awt.headless=false -H:ReflectionConfigurationFiles=conf-dir/reflect-config.json -H:ResourceConfigurationFiles=conf-dir/resource-config.json -H:JNIConfigurationFiles=conf-dir/jni-config.json -jar OpenBLCMM.jar

echo Compiling using native-image
pushd ..\store
call native-image -jar OpenBLCMM.jar

echo Setting icon on EXE
RCEDIT64.exe /I OpenBLCMM.exe ..\windows-processing\openblcmm.ico

echo Collecting into subdir
rd compiled /s /q
md compiled
copy *.dll compiled
copy OpenBLCMM.exe compiled

echo Reporting
dir compiled

popd

