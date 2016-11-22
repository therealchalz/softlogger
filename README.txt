SOFTLOGGER

This is a modbus software-based logging project.  It is able to poll configured modbus devices over
modbus/TCP and modbus/RTU using varying scan rates.  See the softlogger.xml file and the config DTD for
some insight into what can be configured.  The project can output the values 
using custom outputmodules, and there are several basic ones included (a debug 
output module which sends the data to STDOUT, a CSV output module to save to CSV
files, and a DB output module to write realtime values to a DB).

The program uses the Apache Commons Daemon API, meaning that it can be installed
as a daemon on Linux systems or as a service on Windows systems.

The home of this project is currently on github at 
github.com/therealchalz/softlogger

DEPENDENCIES

This program depends on the following libraries:
commons-daemon-1.0.15.jar
log4j-api-2.5.jar
log4j-core-2.5.jar
mysql-connector-java-5.1.38-bin.jar
commons-io-2.4.jar
jssc-2.9.5-experimental.jar (github.com/therealchalz/java-simple-serial-connector)
jamod-1.5.1-therealchalz.jar (github.com/therealchalz/jamod)
brootils-1.2.1.jar (github.com/therealchalz/brootils)
jsvc binary from the commons-daemon project (for linux, version 1.0.15)
procrun binary from the commons-daemon project (for windows, version 1.0.15)

CONFIGURATION

See the sample XML config file as well as the DTD file.

INSTALLATION

Linux

-Put all the *.jar files in /usr/share/java.
-Install the jsvc binary to /usr/bin/jsvc.
-Put put the config files (softlogger.dtd, softlogger.xml, softlogger-log4j2.xml) 
 in /etc/softlogger.  Logs will be saved here as well.
On init.d systems:
-Put the softlogger.init script in /etc/init.d/ (rename it for your convenience)
-Ensure that the softlogger init.d script is executable.
-Install the init.d script with update-rc.d
Or on Systemd systems:
-Put the softlogger.service file in /etc/systemd/system/
-Keep the softlogger.init file in /etc/softlogger
-Enable with 'systemctl --system enable softlogger'

Windows

TODO
