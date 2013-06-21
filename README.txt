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
commons-daemon-1.0.13.jar
log4j-1.2.16.jar
mysql-connector-java-5.1.23-bin.jar
commons-io-2.4.jar
jssc-2.6.0-streams.jar (github.com/therealchalz/java-simple-serial-connector, streams branch)
jamod-1.2-jssc.jar (github.com/therealchalz/jamod-mod)
brootils-1.0.jar (github.com/therealchalz/brootils)
jsvc binary from the commons-daemon project (for linux)
procrun binary from the commons-daemon project (for windows)

CONFIGURATION

See the sample XML config file as well as the DTD file.

INSTALLATION

Linux

Put all the *.jar files in /usr/share/java.
Install the jsvc binary to /usr/local/bin/jsvc.
Put put the config files (softlogger.dtd, softlogger.xml, logger.config) in /etc/softlogger.  Logs 
will be saved here as well.
Put the softlogger.init.d.sh script in /etc/init.d/ (rename it for your convenience)
Ensure that the softlogger init.d script is executable.
Install the init.d script with update-rc.d

Windows

TODO