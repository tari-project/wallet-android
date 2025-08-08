import TariBuildConfig.LibWallet.LibWalletNetwork.ESMERALDA
import TariBuildConfig.LibWallet.LibWalletNetwork.MAINNET
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

tasks.register("downloadLibwallet") {
    val rootDir = "${rootProject.projectDir}/libwallet"

    val requestedTasks = gradle.startParameter.taskNames
    val networkFlavor = when {
        requestedTasks.all { it.contains("mainnet", ignoreCase = true) } -> MAINNET
        requestedTasks.all { it.contains("esmeralda", ignoreCase = true) } -> ESMERALDA
        else -> error(
            "All task must be for the same network. " +
                    "It is required to download network-specific libwallet binaries, which are not compatible with each other."
        )
    }

    inputs.property("version", TariBuildConfig.LibWallet.version + " $networkFlavor")
    outputs.dir(rootDir)

    doLast {
        logger.info("Downloading binaries for version ${TariBuildConfig.LibWallet.version} for $networkFlavor")
        println("Downloading binaries for version ${TariBuildConfig.LibWallet.version} for $networkFlavor")

        val hostURL = "https://github.com/tari-project/tari/releases/download/"
        val archive = when (networkFlavor) {
            MAINNET -> "libminotari_wallet_ffi-mainnet_archive.zip"
            ESMERALDA -> "libminotari_wallet_ffi-esme_archive.zip"
        }
        val archiveUrl = "${hostURL}${TariBuildConfig.LibWallet.version}/${archive}"

        val aarch64dir = when (networkFlavor) {
            MAINNET -> "libminotari_wallet_ffi-mainnet-android-aarch64"
            ESMERALDA -> "libminotari_wallet_ffi-esme-android-aarch64"
        }
        val x86_64dir = when (networkFlavor) {
            MAINNET -> "libminotari_wallet_ffi-mainnet-android-x86_64"
            ESMERALDA -> "libminotari_wallet_ffi-esme-android-x86_64"
        }

        download(archiveUrl, rootDir)
        unzip("$rootDir/$archive", "$rootDir/temp/")

        copyFile(
            fromDir = "$rootDir/temp/$aarch64dir/",
            toDir = "$rootDir/arm64-v8a/",
            oldFileName = "libminotari_wallet_ffi.android_aarch64.a",
            newFileName = "libminotari_wallet_ffi.a",
        )

        copyFile(
            fromDir = "$rootDir/temp/$x86_64dir/",
            toDir = "$rootDir/x86_64/",
            oldFileName = "libminotari_wallet_ffi.android_x86_64.a",
            newFileName = "libminotari_wallet_ffi.a",
        )

        copyFile(
            fromDir = "$rootDir/temp/$x86_64dir/", // wallet.h is the same for both architectures
            toDir = "$rootDir/",
            oldFileName = "libminotari_wallet_ffi.h",
            newFileName = "wallet.h",
        )

        delete(file("$rootDir/temp/"))
        delete {
            delete(fileTree(rootDir).apply {
                include(archive)
            })
        }
    }
}

fun Project.download(fileUrl: String, destPath: String) {
    println("Downloading $fileUrl")
    val destFile = File(destPath)
    ant.invokeMethod("get", mapOf("src" to fileUrl, "dest" to destFile))
    if (!destFile.exists()) {
        throw RuntimeException("Failed to download file from $fileUrl to $destPath")
    }
    println("Downloaded $fileUrl to $destPath")
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

fun unzip(archiveFilePath: String, destPath: String) {
    println("Unzipping $archiveFilePath to $destPath")

    val archiveFile = File(archiveFilePath)
    if (!archiveFile.exists()) {
        println("Archive file not found: $archiveFilePath")
        return
    }

    val destDir = File(destPath)
    if (!destDir.exists()) {
        destDir.mkdirs()
    }

    ZipInputStream(FileInputStream(archiveFile)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val outFile = File(destDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { output ->
                    zip.copyTo(output)
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    println("Unzipping complete for $archiveFilePath")
}
