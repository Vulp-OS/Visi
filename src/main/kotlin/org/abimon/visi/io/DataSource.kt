package org.abimon.visi.io

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Supplier

interface DataSource {
    /**
     * Get an input stream associated with this data source.
     */
    val inputStream: InputStream
    val location: String
    val data: ByteArray
    val size: Long
    
    fun <T> use(action: (InputStream) -> T): T = inputStream.use(action)
}

class FileDataSource(val file: File) : DataSource {

    override val location: String = file.absolutePath

    override val data: ByteArray
        get() = file.readBytes()

    override val inputStream: InputStream
        get() = FileInputStream(file)

    override val size: Long = file.length()
}

class HTTPDataSource(val url: URL, val userAgent: String) : DataSource {
    constructor(url: URL) : this(url, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:44.0) Gecko/20100101 Firefox/44.0")

    override val location: String = url.toExternalForm()

    override val data: ByteArray
        get() {
            val baos = ByteArrayOutputStream()
            use { it.writeTo(baos, 8192, true) }
            return baos.toByteArray()
        }

    override val inputStream: InputStream
        get() {
            val http = url.openConnection() as HttpURLConnection
            http.requestMethod = "GET"
            http.setRequestProperty("User-Agent", userAgent)
            return if (http.responseCode < 400) http.inputStream else http.errorStream
        }

    override val size: Long
        get() = use { it.available().toLong() }
}

class FunctionalDataSource(val dataSupplier: Supplier<ByteArray>) : DataSource {
    override val location: String = "Supplier " + dataSupplier.toString()

    override val data: ByteArray
        get() = dataSupplier.get()

    override val inputStream: InputStream
        get() = ByteArrayInputStream(data)

    override val size: Long
        get() = data.size.toLong()
}

class FunctionDataSource(val dataFunc: () -> ByteArray): DataSource {
    override val location: String = dataFunc.toString()

    override val data: ByteArray
        get() = dataFunc()

    override val inputStream: InputStream
        get() = ByteArrayInputStream(dataFunc())

    override val size: Long
        get() = dataFunc().size.toLong()

}

/** One time use */
class InputStreamDataSource(val stream: InputStream) : DataSource {
    override val location: String = stream.toString()

    override val data: ByteArray
        get() = stream.use { it.readBytes() }

    override val inputStream: InputStream = stream

    override val size: Long
        get() = stream.use { it.available().toLong() }
}