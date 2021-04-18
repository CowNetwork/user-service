# user-service

Account management service using gRPC.

## Environment Variables

| Variable | Default Value | Description |
| -------- | ------------- | ----------- |
| `USER_SERVICE_PORT` | `5816` | The port this server listens to. |
| `USER_SERVICE_POSTGRES_HOST` | `127.0.0.1:5432` | The host of the postgres database. |
| `USER_SERVICE_POSTGRES_DB` | `postgres` | The postgres database used. |
| `USER_SERVICE_POSTGRES_SCHEMA` | `public` | The postgres database schema used. |
| `USER_SERVICE_POSTGRES_USERNAME` | `postgres` | The username to use for postgres connections. |
| `USER_SERVICE_POSTGRES_PASSWORD` | `postgres` | The password to use for postgres connections. |
| `USER_SERVICE_CLOUD_EVENT_SOURCE` | `cow.global.user-service` | The cloud event source identifier. |
| `USER_SERVICE_KAFKA_BROKERS` | `127.0.0.1:9092` | Comma-separated list of kafka brokers to connect to. |
| `USER_SERVICE_KAFKA_PRODUCER_TOPIC` | `cow.global.user` | The topic to publish cloud events on. |
