plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "Thread-Wars-WikiGame"
include("coroutines")
include("executor")
include("completable-future")
include("algo")
