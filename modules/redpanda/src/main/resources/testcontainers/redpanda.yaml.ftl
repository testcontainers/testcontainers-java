# Injected by testcontainers
<#setting boolean_format="c">
<#setting number_format="c">
redpanda:
  admin:
    address: 0.0.0.0
    port: 9644

  kafka_api:
    - address: 0.0.0.0
      name: external
      port: 9092
      authentication_method: ${ kafkaApi.authenticationMethod }

    # This listener is required for the schema registry client. The schema
    # registry client connects via an advertised listener like a normal Kafka
    # client would do. It can't use the other listener because the mapped
    # port is not accessible from within the Redpanda container.
    - address: 0.0.0.0
      name: internal
      port: 9093
      authentication_method: <#if kafkaApi.enableAuthorization >sasl<#else>none</#if>

  advertised_kafka_api:
    - address: ${ kafkaApi.advertisedHost }
      name: external
      port: ${ kafkaApi.advertisedPort }
    - address: 127.0.0.1
      name: internal
      port: 9093

schema_registry:
  schema_registry_api:
  - address: "0.0.0.0"
    name: main
    port: 8081
    authentication_method: ${ schemaRegistry.authenticationMethod }

schema_registry_client:
  brokers:
    - address: localhost
      port: 9093
