# Recommended logback configuration

Testcontainers, and many of the libraries it uses, utilize slf4j for logging. In order to see logs from Testcontainers,
your project should include an SLF4J implementation (Logback is recommended). The following example `logback-test.xml`
should be included in your classpath to show a reasonable level of log output:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="STDOUT"/>
    </root>

    <logger name="org.testcontainers" level="INFO"/>
    <logger name="org.apache.http" level="WARN"/>
    <logger name="com.github.dockerjava" level="WARN"/>
    <logger name="org.zeroturnaround.exec" level="WARN"/>
</configuration>
```
