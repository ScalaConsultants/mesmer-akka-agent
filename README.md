![Scala CI](https://github.com/ScalaConsultants/mesmer-akka-agent/workflows/Scala%20CI/badge.svg)

# Mesmer Akka Agent

# Architecture 

See [overview](https://github.com/ScalaConsultants/mesmer-akka-agent/blob/main/extension_overview.png).

Mesmer Akka Agent is an Akka extension allowing to monitor Akka ecosystem telemetry data and events.

Currently Mesmer Akka Agent supports the following metrics:

### Akka core

- Running actors
- Mailbox size
- Stash size
- Mailbox time
- Processed messages
- Processing time
- Sent messages

### Akka Cluster

- Shards per region
- Reachable nodes
- Unreachable nodes
- Entities per region
- Shard regions on node
- Entities on node
- Nodes down

### Akka HTTP

- Connections
- Requests
- Responses
- Responses 2xx
- Responses 3xx
- Responses 4xx
- Responses 5xx
- Response time 
- Response time 2xx
- Response time 3xx
- Response time 4xx
- Response time 5xx
- Endpoint responses
- Endpoint responses 2xx 
- Endpoint responses 3xx 
- Endpoint responses 4xx 
- Endpoint responses 5xx 
- Endpoint response time 2xx
- Endpoint response time 3xx
- Endpoint response time 4xx
- Endpoint response time 5xx

### Akka Persistence

- Persisted events
- Event persistence time
- Recovery total
- Recovery time
- Snapshots

### Akka Streams

- Running streams
- Running operators per stream
- Running operators
- Stream throughput
- Operator throughput
- Operator processing time

## OpenTelemetry

Mesmer Akka Agent uses [OpenTelemetry](https://opentelemetry.io/) to allow end user to choose where the data will be stored. This means that application using this extension should include OpenTelemetry SDK and configure appropriate exporter. If no exporter is configured, default NOOP exporter is in use.

# Local testing

`test_app` subproject contains an example application that uses Akka Cluster sharding with Mesmer Akka Agent extension.

## Database setup

The application requires a PostgreSQL instance. The database has to contain the schema for Actor Persistence journal. You can find the relevant sql statements in docker/schema.sql.

If you want to run everything with default value you can just run `docker-compose up` in the `docker` directory.

## Application setup

If you're running the database with the default value you can just do `sbt run`.

Otherwise you might need to override the expected values in the application by setting some or all of the following environment variables:
- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5432`)
- `DB_NAME` (default: `akka`)
- `DB_USER` (default: `postgres`)
- `DB_PASS` (default: `12345`)

## New Relic agent

This is not required to run this with NR agent, as this tool functionality should be orthogonal to NR agent. Nonetheless, `test_app` is tested under New Relic Java agent version `6.0.0`.
