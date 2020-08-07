package com.tari.android.wallet.infrastructure.log

import org.joda.time.DateTime
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random

@ExperimentalStdlibApi
class FileTreeTest {

    private val fixture = FilesFixture.default()
    private val fileTree = FileTree.fromDir(fixture.directory)

    @Before
    fun setup() = fixture.clean()

    companion object {
        @JvmStatic
        @AfterClass
        fun tearDownAll() {
            FilesFixture.default().clean()
        }
    }

    @Test
    fun `length, assert that 0 was returned if a directory does not contain files`() {
        assertEquals(0, fileTree.length())
    }

    @Test
    fun `length, assert that 1 was returned if one file is present in resources folder`() {
        fixture.generateRandomLeafFile(count = 1)
        assertEquals(1, fileTree.length())
    }

    @Test
    fun `length, assert that 0 was returned if one directory is present in resources folder`() {
        fixture.generateRandomDirectory(count = 1)
        assertEquals(0, fileTree.length())
    }

    @Test
    fun `length, assert that 2 was returned if 2 directories and 2 files are present in resources folder`() {
        fixture.generateRandomLeafFile(count = 2)
        fixture.generateRandomDirectory(count = 2)
        assertEquals(2, fileTree.length())
    }

    @Test
    fun `bytes, assert that size of 3 files is expected`() {
        val givenFiles = fixture.generateRandomLeafFile(count = 3)
        assertEquals(givenFiles.fold(0L) { acc, file -> acc + file.length() }, fileTree.bytes())
    }

    @Test
    fun `bytes, assert that size of directories and their internals is not counted`() {
        val givenFiles = fixture.generateRandomLeafFile(count = 3)
        fixture.generateRandomDirectory(1)
            .forEach { FilesFixture(it).generateRandomLeafFile(count = 1) }
        assertEquals(givenFiles.fold(0L) { acc, file -> acc + file.length() }, fileTree.bytes())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `shrink, assert that IllegalArgumentException was thrown if targetSize is negative`() {
        fileTree.shrink(-1L)
    }

    @Test
    fun `shrink, assert that 4 out of 5 (the oldest) files were deleted if each consumes 50 bytes of space and target size if 170`() {
        val givenFiles = fixture.generateRandomLeafFile(count = 5, size = 50)
        val lastModifiedTime = DateTime(2005 + (givenFiles.size - 1) * 5, 1, 1, 0, 0).millis
        givenFiles.withIndex()
            .map { Pair(it.value, DateTime(2005 + it.index * 5, 1, 1, 0, 0).millis) }
            .forEach { (file, time) -> file.setLastModified(time) }
        fileTree.shrink(3 * 50 + 20)
        assertEquals(1, fileTree.length())
        assertEquals(lastModifiedTime, givenFiles.single(File::exists).lastModified())
    }

    @Test
    fun `shrink, assert that lastFileHandler was not called if last file was not a subject of deletion`() {
        fixture.generateRandomLeafFile(count = 5, size = 50)
        fileTree.shrink(50) { throw RuntimeException() }
    }

    @Test
    fun `shrink, assert that lastFileHandler was called if last file was a subject of deletion`() {
        fixture.generateRandomLeafFile(count = 5, size = 50)
        fileTree.shrink(220)
        assertEquals(0, fileTree.length())
    }

    @Test
    fun `shrink, assert that last file was not deleted if lastFileHandler is overridden in such a way`() {
        fixture.generateRandomLeafFile(count = 5, size = 50)
        fileTree.shrink(220) { /*No-Op*/ }
        assertEquals(1, fileTree.length())
    }

    @Test
    fun `shrink, assert that last filetree's total space is 0 if the last file to be deleted was cleared out instead`() {
        fixture.generateRandomLeafFile(count = 5, size = 50)
        fileTree.shrink(220) { it.writeBytes(ByteArray(0)) }
        assertEquals(0, fileTree.bytes())
    }

    private class FilesFixture(val directory: File) {

        init {
            if (!directory.exists()) directory.mkdir()
            require(directory.isDirectory) { "Failed argument: $directory" }
        }

        fun generateRandomLeafFile(count: Int, size: Int = 0) =
            generateRandomFile(count) {
                it.createNewFile()
                if (size != 0) it.writeBytes(Random.nextBytes(size))
            }

        fun generateRandomDirectory(count: Int) = generateRandomFile(count, File::mkdir)

        private fun generateRandomFile(count: Int, block: (File) -> Any): List<File> =
            generateSequence { File(directory, Random.nextLong().toString()) }
                .takeWhile { !it.exists() }
                .onEach { block(it) }
                .take(count)
                .toList()

        fun clean() {
            for (it in directory.listFiles() ?: emptyArray()) it.deleteRecursively()
        }

        companion object {
            fun default() = FilesFixture(Paths.get("src", "test", "resources").toFile())
        }
    }

}
