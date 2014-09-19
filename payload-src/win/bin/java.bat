@echo off
REM execute KNIME's java to run PIA with the commands specified in the CTDs
"%KNIME_JAVA%" -cp "%PIA_RUNNABLE%" %*
