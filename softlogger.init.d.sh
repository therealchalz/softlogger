#! /bin/sh

# Put JSVC in /usr/bin
# Put softlogger.jar and all of the required libs in /usr/share/java
# Put required config files and stuff in /etc/softlogger

PROCESS_NAME=softlogger
START_CLASS=ca.brood.softlogger.Softlogger
HOME_DIR=/etc/softlogger
PID_FILE=/var/run/softlogger.pid

CPHOME=/usr/share/java
JSVC=/usr/bin/jsvc
JVM=/usr/lib/jvm/default-java

CP=$CPHOME/activation.jar:\
$CPHOME/brootils-1.0.jar:\
$CPHOME/commons-daemon-1.0.13.jar:\
$CPHOME/commons-io-2.4.jar:\
$CPHOME/Jama-1.0.2.jar:\
$CPHOME/jamod-1.2-jssc.jar:\
$CPHOME/jep-2.4.1.jar:\
$CPHOME/jsch-0.1.49.jar:\
$CPHOME/jssc-2.6.0-streams.jar:\
$CPHOME/junit.jar:\
$CPHOME/log4j-1.2.16.jar:\
$CPHOME/mail.jar:\
$CPHOME/mysql-connector-java-5.1.23-bin.jar:\
$CPHOME/softlogger-1.1.jar:

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
