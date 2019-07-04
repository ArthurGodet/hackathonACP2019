@echo off

if exist "HackathonACP2019.jar" ( 
del HackathonACP2019.jar
)
mvn clean package
cd target
copy HackathonACP2019.jar ..\HackathonACP2019.jar
cd ..