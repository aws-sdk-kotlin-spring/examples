plugins {
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(libs.awskotlinSqsStarter)

    // kotlinx converter runtime and @Serializable example payloads
    implementation(libs.kotlinxSerializationJson)
}