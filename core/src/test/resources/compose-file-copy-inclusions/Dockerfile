FROM jbangdev/jbang-action

WORKDIR /app
COPY EnvVariableRestEndpoint.java .

RUN jbang export portable --force EnvVariableRestEndpoint.java

EXPOSE 8080
CMD ["./EnvVariableRestEndpoint.java"]
