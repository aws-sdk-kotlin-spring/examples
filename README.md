# Examples

[![Build](https://github.com/aws-sdk-kotlin-spring/examples/actions/workflows/build.yml/badge.svg)](https://github.com/aws-sdk-kotlin-spring/examples/actions/workflows/build.yml)

Runnable Spring Boot examples for the [awskotlin](https://github.com/aws-sdk-kotlin-spring/awskotlin) modules.
Each service lives in its own self-contained module with a dedicated application, configuration and LocalStack integration test.

## Overview

| Module | Service | Starter | Demonstrates |
|--------|---------|---------|--------------|
| `s3-example` | S3 | `awskotlin-s3-spring-boot-starter` | `S3Template` bucket/object operations behind a `StorageService` |
| `sqs-example` | SQS | `awskotlin-sqs-spring-boot-starter` | `SqsTemplate` send/receive and an `@SqsListener` annotation-driven receiver |
| `sns-example` | SNS | `awskotlin-sns-spring-boot-starter` (+ SQS) | Topic publish and SNS → SQS fan-out: queue subscription, envelope parsing with `SnsNotificationParser` in an `@SqsListener` |
| `secretsmanager-example` | Secrets Manager | `awskotlin-secrets-manager-spring-boot-starter` | Secret retrieval and creation behind a `SecretsManagementService` |

Module directories carry the `-example` suffix; the Gradle project names drop it, so task paths are `:s3`, `:sqs`, `:sns` and `:secretsmanager`.

## Prerequisites

- JDK 21+
- Docker (LocalStack for the integration tests)
- GitHub Packages credentials to resolve the awskotlin snapshots: set `gpr.user` and `gpr.key` (a PAT with `read:packages`) in `~/.gradle/gradle.properties`. CI uses the built-in `GITHUB_TOKEN`. Alternatively, build awskotlin locally (`./gradlew publishToMavenLocal` in the awskotlin repo) — the examples resolve snapshots from `mavenLocal()` first.

## Build and test

```bash
./gradlew build
```

Or a single module:

```bash
./gradlew :s3:build
```

The tests start LocalStack with Testcontainers (Docker required), each scoped to the services its example uses. LocalStack 4 needs a license token; set `LOCALSTACK_AUTH_TOKEN` to run those tests — they are skipped when the token is absent.

## Run

Start LocalStack and run one of the example applications:

```bash
docker compose up -d localstack
./gradlew :s3:bootRun
./gradlew :sqs:bootRun
./gradlew :sns:bootRun
./gradlew :secretsmanager:bootRun
```

Each app is preconfigured for LocalStack in its own `src/main/resources/application.properties` (`awskotlin.region` and static test credentials; the S3 example also sets `example.s3.bucket-name`). The SQS example creates its `default` queue on startup; the SNS example wires a `notifications` topic → queue fan-out so its listener can consume published notifications.

## Stack

Spring Boot 4.2.0, Kotlin 2.4.21, AWS SDK for Kotlin 1.9.45, Java 21, Gradle 9.9.2.
