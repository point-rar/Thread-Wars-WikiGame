plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "wikigame"
include("coroutines")
include("executor")
include("completable-future")
