#!/bin/bash
# vim: set expandtab tabstop=4 shiftwidth=4:

export JAVA_HOME="/home/deck/bellsoft-liberica-vm-core-openjdk17-23.0.1"
export PATH="${JAVA_HOME}/bin:${PATH}"

# The default when compiling on AMD, and the one we'll be using
export NIKARCH="x86-64-v3"
# Compatibility; works on the widest possible range of CPUs
#export NIKARCH="compatibility"
# Native -- use anything useful found on the compiling CPU
#export NIKARCH="native"

# Check for dependencies
for COMMAND in native-image
do
    if [ ! -x "$(command -v ${COMMAND})" ]; then
        echo "JAVA_HOME was set to: ${JAVA_HOME}"
        echo "${COMMAND} not found on path, exiting..."
        exit 1
    fi
done

# Get into the correct dir
cd ../store

# Compile!
echo "Compiling..."
echo
native-image -march=${NIKARCH} -jar OpenBLCMM.jar
echo

# Move stuff into its own dir
echo "Collecting into subdir"
echo
rm -rf steamos
mkdir steamos
mv *.so OpenBLCMM steamos/
echo

# ... and we're out!
echo "Done!"
echo

# Display the results
ls -l steamos/

