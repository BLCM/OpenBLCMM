@echo off

pushd ..\store
java -agentlib:native-image-agent=config-output-dir=../src/META-INF/native-image/blcmm/blcmm -jar OpenBLCMM.jar
popd

