@echo off
javac  -cp src/* -d "./bin"
java -cp "./bin" OldPolish
pause