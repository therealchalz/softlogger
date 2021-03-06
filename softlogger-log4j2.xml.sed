<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
	<Appenders>
		<Console name="Console" target="SYSTEM_OUT">
			<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} = %msg%n"/>
		</Console>
		<RollingRandomAccessFile name="RollingRandomAccessFile" fileName="SEDLOGDIRECTORYsoftlogger.log" filePattern="SEDLOGDIRECTORY$${date:yyyy-MM}/softlogger-%d{MM-dd-yyyy}-%i.log.gz">
			<PatternLayout>
				<Pattern>%d %p %c{1.} [%t] %m%n</Pattern>
			</PatternLayout>
			<Policies>
				<TimeBasedTriggeringPolicy />
				<SizeBasedTriggeringPolicy size="20M"/>
			</Policies>
			<DefaultRolloverStrategy max="300"/>
		</RollingRandomAccessFile>
	</Appenders>
	<Loggers>
		<Root level="trace">
			<AppenderRef ref="Console"/>
			<AppenderRef ref="RollingRandomAccessFile"/>
		</Root>
	</Loggers>
</Configuration>
