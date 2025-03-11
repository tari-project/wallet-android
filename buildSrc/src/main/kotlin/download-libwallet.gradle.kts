/**
 * Downloads JNI binaries as version specified in the BuildConfig file
 */
tasks.register("downloadLibwallet") {
    val rootDir = "${rootProject.projectDir}/libwallet/"
    val armDir = "${rootProject.projectDir}/libwallet/arm64-v8a/"
    val x64Dir = "${rootProject.projectDir}/libwallet/x86_64/"

    inputs.property("version", TariBuildConfig.LibWallet.version)
    outputs.dir(rootDir)

    doLast {
        logger.info("Downloading binaries with version ${TariBuildConfig.LibWallet.version}")
        println("Downloading binaries with version ${TariBuildConfig.LibWallet.version}")

        val files = listOf(
            TariBuildConfig.LibWallet.header,
            TariBuildConfig.LibWallet.armA,
            TariBuildConfig.LibWallet.x64A,
        )

        download(TariBuildConfig.LibWallet.headerFileUrl, rootDir)
        copyFile(rootDir, rootDir, TariBuildConfig.LibWallet.header, "wallet.h")

        download(TariBuildConfig.LibWallet.x64AFileUrl, rootDir)
        copyFile(rootDir, x64Dir, TariBuildConfig.LibWallet.x64A, "libminotari_wallet_ffi.a")

        download(TariBuildConfig.LibWallet.armAFileUrl, rootDir)
        copyFile(rootDir, armDir, TariBuildConfig.LibWallet.armA, "libminotari_wallet_ffi.a")

        delete {
            delete(fileTree(rootDir).apply {
                include(files)
            })
        }
    }
}

fun Project.download(fileUrl: String, destPath: String) {
    println("Downloading $fileUrl")
    val destFile = File(destPath)
    ant.invokeMethod("get", mapOf("src" to fileUrl, "dest" to destFile))
}

fun Project.copyFile(fromDir: String, toDir: String, oldFileName: String, newFileName: String): WorkResult {
    println("Copying $oldFileName to $toDir$newFileName")
    return copy {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("$fromDir$oldFileName")
        rename { currentFileName -> if (currentFileName == oldFileName) newFileName else currentFileName }
        into(toDir)
    }
}
