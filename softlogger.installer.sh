#!/bin/bash

# This script will install the softlogger using the following settings

LOGOUTPUTDIR=logs/
#LOGOUTPUTDIR=/persistent/softlogger/logs/
INSTALLDIR=/etc/softlogger/
JAROUTDIR=/usr/share/java/


JARINDIR=lib/
JARS="brootils-1.2.1.jar commons-daemon-1.0.15.jar commons-io-2.4.jar jamod-1.5.1-therealchalz.jar jep-2.4.1.jar jsch-0.1.53.jar jssc-2.9.5-experimental.jar log4j-api-2.5.jar log4j-core-2.5.jar mysql-connector-java-5.1.38-bin.jar softlogger-1.4.2.jar"

COREFILES="LICENSE.txt README.txt softlogger.dtd softlogger.init softlogger.xml softlogger-log4j2.xml"

echo "Moving required jar files from $JARINDIR to $JAROUTDIR"
pushd $JARINDIR
chown root:root $JARS
mv $JARS $JAROUTDIR
popd

echo "Configuring softlogger.init and softlogger.service scripts"
sed "s~SEDHOMEDIR~$INSTALLDIR~g" < softlogger.init.sed > softlogger.init
sed "s~SEDHOMEDIR~$INSTALLDIR~g" < softlogger.service.sed > softlogger.service
chmod +x softlogger.init

echo "Configuring softlogger-log4j2.xml"
sed "s~SEDLOGDIRECTORY~$LOGOUTPUTDIR~g" < softlogger-log4j2.xml.sed > softlogger-log4j2.xml

echo "Moving core files to $INSTALLDIR"
mkdir -p $INSTALLDIR
chown root:root $COREFILES
mv $COREFILES $INSTALLDIR

echo "Installing up systemd service file"
chown root:root softlogger.service
mv softlogger.service /etc/systemd/system/

echo "Steps required to complete installation:"
echo "  - Configure softlogger.xml as required"
echo "  - Ensure that any required lookup tables are in the required directory"
echo "  - Enable the soflogger at boot with 'systemctl --system enable softlogger'"
