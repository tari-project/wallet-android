@file:Suppress("UnstableApiUsage")

include(":app", ":yatlib")
rootProject.name = "Tari Wallet"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://giphy.bintray.com/giphy-sdk") }
        maven { url = uri("https://raw.githubusercontent.com/guardianproject/gpmaven/master") }
        maven { url = uri("https://jitpack.io") }
    }
}
