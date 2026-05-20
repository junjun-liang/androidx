plugins {
    id 'java-library'
    id 'org.jetbrains.kotlin.jvm'
}

dependencies {
    implementation project(':compose:remote:remote-core')
    implementation libs.gson
    implementation libs.kotlinx.coroutines.core
}
