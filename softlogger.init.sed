#! /bin/sh

# Put JSVC in /usr/bin
# Put softlogger.jar and all of the required libs in /usr/share/java
# Put required config files and stuff in /etc/softlogger
# On init.d systems, this file (softlogger.init) can be used in /etc/init.d/
# On systemd systems, install softlogger.service to /etc/systemd/system/ and
# enable it with 'systemctl --system enable softlogger'

PROCESS_NAME=softlogger
START_CLASS=ca.brood.softlogger.Softlogger
HOME_DIR=SEDHOMEDIR
PID_FILE=/var/run/softlogger.pid

CPHOME=/usr/share/java
JSVC=/usr/bin/jsvc
JVM=/usr/lib/jvm/default-java

CP=$CPHOME/brootils-1.2.1.jar:\
$CPHOME/commons-daemon-1.0.15.jar:\
$CPHOME/commons-io-2.4.jar:\
$CPHOME/jamod-1.5.1-therealchalz.jar:\
$CPHOME/jep-2.4.1.jar:\
$CPHOME/jsch-0.1.53.jar:\
$CPHOME/jssc-2.9.5-experimental.jar:\
$CPHOME/log4j-api-2.5.jar:\
$CPHOME/log4j-core-2.5.jar:\
$CPHOME/mysql-connector-java-5.1.38-bin.jar:\
$CPHOME/softlogger-1.4.2.jar:

case "$1" in
        start)
                echo -n "Starting daemon "
        $JSVC -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        stop)
                echo -n "Shutting down daemon "
        $JSVC -stop -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        restart)
                ## Stop the service and regardless of whether it was
                ## running or not, start it again.
                $0 stop
                $0 start
                ;;
        *)
                ## If no parameters are given, print which are avaiable.
                echo "Usage: $0 {start|stop|restart}"
                exit 1
                ;;
esac
