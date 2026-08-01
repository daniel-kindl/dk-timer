package dev.danielkindl.ocho.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateDownloaderStatusTest {

    @Test
    fun `computes percent for a partial download`() {
        assertEquals(50, computeDownloadPercent(downloadedBytes = 50, totalBytes = 100))
    }

    @Test
    fun `rounds down fractional percent`() {
        assertEquals(33, computeDownloadPercent(downloadedBytes = 1, totalBytes = 3))
    }

    @Test
    fun `returns 100 when the download is complete`() {
        assertEquals(100, computeDownloadPercent(downloadedBytes = 100, totalBytes = 100))
    }

    @Test
    fun `guards against division by zero when total is zero`() {
        assertEquals(0, computeDownloadPercent(downloadedBytes = 0, totalBytes = 0))
    }

    @Test
    fun `returns 0 when nothing has downloaded yet`() {
        assertEquals(0, computeDownloadPercent(downloadedBytes = 0, totalBytes = 500))
    }
}
