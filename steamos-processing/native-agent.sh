#!/bin/bash
# vim: set expandtab tabstop=4 shiftwidth=4:

export JAVA_HOME="/home/deck/bellsoft-liberica-vm-core-openjdk17-23.0.1"
export PATH="${JAVA_HOME}/bin:${PATH}"

# This can help fonts look better while running, though it does *not* actually
# help anything on the Steam Deck.
export FREETYPE_PROPERTIES="truetype:interpreter-version=35"

# Check for dependencies
for COMMAND in jq unix2dos java native-image
do
    if [ ! -x "$(command -v ${COMMAND})" ]; then
        echo "JAVA_HOME was set to: ${JAVA_HOME}"
        echo "${COMMAND} not found on path, exiting..."
        exit 1
    fi
done

# Get into the correct dir
cd ../store

# Run the agent
echo "Running Native Image Agent..."
echo
java -agentlib:native-image-agent=config-merge-dir=../src/META-INF/native-image/blcmm/blcmm -jar OpenBLCMM.jar
echo

# Normalize JSON (w/ compat for Windows-generated JSON)
echo "Running JSON through jq..."
echo
cd ../src/META-INF/native-image/blcmm/blcmm
for file in *.json
do
    jq . < ${file} | unix2dos > ${file}.new
    mv ${file}.new ${file}
done

# ... and we're out!
echo "Done!"
echo

