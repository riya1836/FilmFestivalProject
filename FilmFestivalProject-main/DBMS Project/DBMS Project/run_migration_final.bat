@echo off
cd /d "%~dp0"
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root --password=root123 film_festival < db_migration_rbac_final.sql
pause
