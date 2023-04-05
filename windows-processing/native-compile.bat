@echo off

Rem Original commandline, prior to pulling that config into the jarfile
Rem native-image -Djava.awt.headless=false -H:ReflectionConfigurationFiles=conf-dir/reflect-config.json -H:ResourceConfigurationFiles=conf-dir/resource-config.json -H:JNIConfigurationFiles=conf-dir/jni-config.json -jar OpenBLCMM.jar

native-image -jar OpenBLCMM.jar

