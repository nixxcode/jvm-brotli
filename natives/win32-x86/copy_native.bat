cd "%~dp0"
SET TARGET_CLASSES_PATH=%~dp0target\classes\lib\win32-x86
if not exist "%TARGET_CLASSES_PATH%" mkdir "%TARGET_CLASSES_PATH%"
copy /Y "brotli.dll" "%TARGET_CLASSES_PATH%" || goto ERROR
goto :EOF

:ERROR
cd %~dp0
echo "*** An error occured. Please check log messages. ***"
exit /b -1