package com.tari.android.wallet.infrastructure.log

import java.io.File

class FileTree private constructor(private val workingDir: File) {

    fun bytes(): Long = filesSequence().sumByLong(File::length)

    fun length() = filesSequence().count()

    @ExperimentalStdlibApi
    fun shrink(
        targetSize: Long,
        comparator: Comparator<File> = UpdateTimeComparator,
        lastFileHandler: (File) -> Unit = { it.delete() }
    ) {
        require(targetSize >= 0L) { "targetSize can't be negative but had $targetSize" }
        if (bytes() > targetSize) {
            val files = filesSequence()
                .sortedWith(comparator)
                .scan(mutableListOf<File>() to 0L) { (files, totalLength), file ->
                    files.apply { add(file) } to totalLength + file.length()
                }
                .first { it.second >= targetSize }
                .first
            val lastFileIndex = listFiles().size - 1
            files.withIndex()
                .forEach { (index, file) ->
                    if (index == lastFileIndex) file.apply(lastFileHandler) else file.delete()
                }
        }
    }

    private fun filesSequence() =
        listFiles().asSequence().filterNotNull().filter(File::isFile)

    private fun listFiles(): Array<File> = (workingDir.listFiles() ?: emptyArray())

    private inline fun <T> Sequence<T>.sumByLong(selector: (T) -> Long): Long {
        var sum = 0L
        for (element in this) sum += selector(element)
        return sum
    }

    object UpdateTimeComparator : Comparator<File> {
        override fun compare(o1: File?, o2: File?): Int = when {
            o1 == null && o2 == null -> 0
            o1 == null -> -1
            o2 == null -> 1
            else -> o1.lastModified().compareTo(o2.lastModified())
        }
    }

    companion object {
        fun fromDir(directory: File): FileTree {
            require(directory.isDirectory) { "Given file is not a directory: $directory" }
            return FileTree(directory)
        }
    }

}
