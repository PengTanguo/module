package jp.john.https


import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class PlatformHttpClient {
    actual suspend fun execute(request: HttpRequest): HttpResponse {
        return suspendCancellableCoroutine { continuation ->
            val url = NSURL(string = request.buildUrl())
            val urlRequest = NSMutableURLRequest(uRL = url)

            urlRequest.HTTPMethod = request.method.name
            urlRequest.timeoutInterval = request.timeout / 1000.0

            // 设置请求头
            request.headers.forEach { (key, value) ->
                urlRequest.setValue(value, forHTTPHeaderField = key)
            }

            // 设置请求体
            if (request.body != null) {
                urlRequest.HTTPBody = request.body.encodeToByteArray()
                    .toNSData()
            }

            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(urlRequest) { data, response, error ->
                if (error != null) {
                    continuation.resume(
                        HttpResponse(
                            statusCode = -1,
                            headers = emptyMap(),
                            body = null,
                            errorMessage = error.localizedDescription
                        )
                    )
                    return@dataTaskWithRequest
                }

                val httpResponse = response as? NSHTTPURLResponse
                val statusCode = httpResponse?.statusCode?.toInt() ?: -1

                val headers = (httpResponse?.allHeaderFields as? Map<*, *>)
                    ?.mapKeys { it.key.toString() }
                    ?.mapValues { it.value.toString() }
                    ?: emptyMap()

                val body = data?.let {
                    NSString.create(data = it, encoding = NSUTF8StringEncoding) as String
                }

                continuation.resume(
                    HttpResponse(
                        statusCode = statusCode,
                        headers = headers,
                        body = body
                    )
                )
            }

            task.resume()

            continuation.invokeOnCancellation {
                task.cancel()
            }
        }
    }
}

private fun ByteArray.toNSData(): NSData {
    return NSMutableData().apply {
        if (isNotEmpty()) {
            this@toNSData.usePinned {
                appendBytes(it.addressOf(0), size.toULong())
            }
        }
    }
}
