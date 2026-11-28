dependencies {
    implementation(libs.awskotlinSnsStarter)

    // SNS has no receive API; the fan-out example consumes notifications through SQS.
    implementation(libs.awskotlinSqsStarter)
}