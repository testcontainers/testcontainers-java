@echo off
if not "%1" == "get" (
    exit 1
)

echo {
echo   "ServerURL": "url",
echo   "Username": "username",
echo   "Secret": "secret"
echo }
