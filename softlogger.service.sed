#########
# /etc/systemd/system/softlogger.service 
#########
[Unit]
Description=Softlogger

[Service]
Type=forking
ExecStart=SEDHOMEDIR/softlogger.init start
ExecStop=SEDHOMEDIR/softlogger.init stop

[Install]
WantedBy=multi-user.target
