package jp.john.https


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformHttpClient {
    actual suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(request.buildUrl())
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = request.method.name
            connection.connectTimeout = request.timeout.toInt()
            connection.readTimeout = request.timeout.toInt()
            connection.doInput = true

            // 设置请求头
            request.headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            // 设置请求体
            if (request.body != null && request.method != HttpMethod.GET) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(request.body)
                    writer.flush()
                }
            }

            // 获取响应
            val statusCode = connection.responseCode
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key!! }
                .mapValues { it.value.firstOrNull() ?: "" }

            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.readText()
                }
            }

            HttpResponse(
                statusCode = statusCode,
                headers = headers,
                body = body
            )
        } catch (e: Exception) {
            HttpResponse(
                statusCode = -1,
                headers = emptyMap(),
                body = null,
                errorMessage = e.message
            )
        } finally {
            connection?.disconnect()
        }
    }
}
