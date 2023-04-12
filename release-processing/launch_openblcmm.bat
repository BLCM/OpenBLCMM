@echo off

Rem To change how much RAM OpenBLCMM is using, change the -Xmx argument, or
Rem create your own launch script with a different parameter.  The default
Rem in here allocates 2GB.  You can also use "M" as a suffix to supply more
Rem precise values.  Note that due to some peculiarities of Java memory
Rem allocation, setting this value too high might result in the app not
Rem launching properly, even if you technically have enough RAM free.

java -Xmx2G -jar OpenBLCMM.jar
