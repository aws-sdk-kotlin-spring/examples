plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "examples"

include("s3-example")
include("sqs-example")
include("sns-example")
include("secretsmanager-example")

// Directories keep the -example suffix; the project names drop it.
project(":s3-example").name = "s3"
project(":sqs-example").name = "sqs"
project(":sns-example").name = "sns"
project(":secretsmanager-example").name = "secretsmanager"