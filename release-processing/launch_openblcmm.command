#!/bin/bash
# vim: set expandtab tabstop=4 shiftwidth=4:

# Head into the dir containing the shell script (OSX will otherwise
# start out in the user's homedir)
cd -- "$(dirname "$BASH_SOURCE")"

# To change how much RAM OpenBLCMM is using, change the -Xmx argument, or
# create your own launch script with a different parameter.  The default
# in here allocates 2GB.  You can also use "M" as a suffix to supply more
# precise values.  Note that due to some peculiarities of Java memory
# allocation, setting this value too high might result in the app not
# launching properly, even if you technically have enough RAM free.

java -Xmx2G -jar OpenBLCMM.jar
