@echo off

Rem Make sure we have jq-win64, for postprocessing
where /q jq-win64
if ERRORLEVEL 1 (
    echo jq-win64 not found!
    echo install it with 'winget install jqlang.jq', and restart your
    echo shell to make sure it's in your path.  Aborting agent run...
    exit /B
)

Rem Now get to it.
pushd ..\store
echo Running Native Image Agent...
echo.
java -agentlib:native-image-agent=config-output-dir=../src/META-INF/native-image/blcmm/blcmm -jar OpenBLCMM.jar
echo.

echo Running JSON through jq...
echo.
cd ..\src\META-INF\native-image\blcmm\blcmm
for %%f in (*.json) do (
    echo %%f
    jq-win64 . %%f > %%f.new
    move /Y %%f.new %%f
)
echo.

echo Done!
echo.

popd

