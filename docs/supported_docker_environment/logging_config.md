# Recommended logback configuration

Testcontainers, and many of the libraries it uses, utilize SLF4J for logging. In order to see logs from Testcontainers,
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
    <!-- The following logger can be used for containers logs since 1.18.0 -->
    <logger name="tc" level="INFO"/>
    <logger name="com.github.dockerjava" level="WARN"/>
    <!--
      Testcontainers talks to the Docker daemon over a shaded copy of Apache HttpClient.
      These loggers print the full HTTP traffic, including credentials, and must stay OFF
      unless explicitly enabled. The names cover both the shaded copy bundled with
      Testcontainers and any plain (unshaded) Apache HttpClient 5.x on your classpath.
    -->
    <logger name="org.apache.hc.client5.http.wire" level="OFF"/>
    <logger name="org.apache.hc.client5.http.headers" level="OFF"/>
</configuration>
```

In order to troubleshoot issues with Testcontainers, increase the logging level of `org.testcontainers` to `DEBUG`:

```xml
<logger name="org.testcontainers" level="DEBUG"/>
```

Raising `org.testcontainers` (or `com.github.dockerjava`) to `DEBUG` does not turn on HTTP wire logging for the
communication with the Docker daemon. That logging is off by default and opt-in only, because the wire logs include
complete HTTP request and response headers and bodies, including registry credentials (e.g. the Base64-encoded
`X-Registry-Auth` header used for image pulls). To enable it temporarily for troubleshooting, explicitly set the exact
logger names to `DEBUG`:

```xml
<logger name="org.apache.hc.client5.http.wire" level="DEBUG"/>
<!-- or, for request/response headers only: -->
<logger name="org.apache.hc.client5.http.headers" level="DEBUG"/>
```

The same logger names apply whether Testcontainers is using its shaded copy of Apache HttpClient (from the published
jar) or a plain Apache HttpClient 5.x (e.g. when running from a source classpath), and they also control wire logging
for other Apache HttpClient 5.x usage in your application.

Avoid changing the root logger's level to `DEBUG`, because this turns on debug logging for every package whose level
isn't explicitly configured here — resulting in a large amount of log data and, in particular, in wire logging of the
Docker API communication (see above), which includes credentials.
