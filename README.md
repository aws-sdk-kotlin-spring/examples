# awskotlin Examples

[![Build](https://github.com/aws-sdk-kotlin-spring/examples/actions/workflows/build.yml/badge.svg)](https://github.com/aws-sdk-kotlin-spring/examples/actions/workflows/build.yml)

Runnable Spring Boot examples for the [awskotlin](https://github.com/aws-sdk-kotlin-spring/awskotlin) modules.

## Overview

| Service | Starter | Demonstrates |
|---------|---------|--------------|
| S3 | `awskotlin-s3-spring-boot-starter` | `S3Template` bucket/object operations behind a `StorageService` |
| Secrets Manager | `awskotlin-secrets-manager-spring-boot-starter` | Secret retrieval behind a `SecretsManagementService` |
| SQS | `awskotlin-sqs-spring-boot-starter` | `SqsTemplate` send/receive and an `@SqsListener` annotation-driven receiver |

## Prerequisites

- JDK 21+
- Docker (LocalStack for the integration tests)
- GitHub Packages credentials to resolve the awskotlin snapshots: set `gpr.user` and `gpr.key` (a PAT with `read:packages`) in `~/.gradle/gradle.properties`. CI uses the built-in `GITHUB_TOKEN`.

## Build and test

```bash
./gradlew build
```

The tests start LocalStack with Testcontainers (Docker required). LocalStack 4 needs a license token; set `LOCALSTACK_AUTH_TOKEN` to run those tests — they are skipped when the token is absent.

## Run

Start LocalStack and run the application:

```bash
docker compose up -d localstack
./gradlew bootRun
```

The app is preconfigured for LocalStack in `src/main/resources/application.properties` (`awskotlin.region`, static test credentials, and the example S3 bucket name).

## Stack

Spring Boot 4.2.0, Kotlin 2.4.21, AWS SDK for Kotlin 1.9.45, Java 21, Gradle 9.9.2.