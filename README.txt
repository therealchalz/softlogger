SOFTLOGGER

This is a modbus software-based logging project.  It is able to poll configured modbus devices over
modbus/TCP and modbus/RTU using varying scan rates.  See the config xml file and the config DTD for
some insight into what can be configured.  The project can output the values using custom output
modules, and there are several basic ones included (a debug output module which sends the data to
STDOUT, a CSV output module to save to CSV files, and a DB output module to write realtime values
to a DB).

The program uses the Apache Commons Daemon API, meaning that it can be installed as a daemon on 
Linux systems or as a service on Windows systems.

This project is in beta but is seeing limited production use and testing.

The home of this project is currently on github at github.com/therealchalz/softlogger

DEPENDENCIES

This program depends on the following libraries:
commons-daemon-1.0.15.jar
log4j-api-2.5.jar
log4j-core-2.5.jar
mysql-connector-java-5.1.38-bin.jar
commons-io-2.4.jar
jssc-2.9-therealchalz.jar (github.com/therealchalz/java-simple-serial-connector)
jamod-1.5-therealchalz.jar (github.com/therealchalz/jamod)
brootils-1.1.jar (github.com/therealchalz/brootils)
jsvc binary from the commons-daemon project (for linux, version 1.0.15)
procrun binary from the commons-daemon project (for windows, version 1.0.15)

CONFIGURATION

See the sample XML config file as well as the DTD file.

INSTALLATION

Linux

Put all the *.jar files in /usr/share/java.
Install the jsvc binary to /usr/bin/jsvc.
Put put the config files (softlogger.dtd, softlogger.xml) in /etc/softlogger.  Logs 
will be saved here as well.
Put the softlogger.init.d.sh script in /etc/init.d/ (rename it for your convenience)
Ensure that the softlogger init.d script is executable.
Install the init.d script with update-rc.d

Windows

TODO