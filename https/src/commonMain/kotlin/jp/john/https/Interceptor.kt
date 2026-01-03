package jp.john.https


/**
 * 拦截器接口
 */
interface Interceptor {
    suspend fun intercept(chain: Chain): HttpResponse

    interface Chain {
        val request: HttpRequest
        suspend fun proceed(request: HttpRequest): HttpResponse
    }
}

/**
 * 真实的调用链
 */
class RealInterceptorChain(
    private val interceptors: List<Interceptor>,
    private val index: Int,
    override val request: HttpRequest,
    private val call: suspend (HttpRequest) -> HttpResponse
) : Interceptor.Chain {

    override suspend fun proceed(request: HttpRequest): HttpResponse {
        if (index >= interceptors.size) {
            return call(request)
        }

        val next = RealInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
            request = request,
            call = call
        )

        return interceptors[index].intercept(next)
    }
}

/**
 * 日志拦截器示例
 */
class LoggingInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): HttpResponse {
        val request = chain.request
        println("==> ${request.method} ${request.buildUrl()}")
        println("Headers: ${request.headers}")
        request.body?.let { println("Body: $it") }

        val startTime = currentTimeMillis()
        val response = chain.proceed(request)
        val duration = currentTimeMillis() - startTime

        println("<== ${response.statusCode} (${duration}ms)")
        println("Response: ${response.body}")

        return response
    }

    private fun currentTimeMillis(): Long {
        return kotlin.system.getTimeMillis()
    }
}

/**
 * Header 拦截器示例
 */
class HeaderInterceptor(
    private val headers: Map<String, String>
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): HttpResponse {
        val originalRequest = chain.request
        val newHeaders = originalRequest.headers.toMutableMap().apply {
            putAll(headers)
        }
        val newRequest = originalRequest.copy(headers = newHeaders)
        return chain.proceed(newRequest)
    }
}

/**
 * 重试拦截器
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelay: Long = 1000L
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): HttpResponse {
        var lastResponse: HttpResponse? = null
        var attempt = 0

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request)
                if (response.isSuccessful) {
                    return response
                }
                lastResponse = response
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    return HttpResponse(
                        statusCode = -1,
                        headers = emptyMap(),
                        body = null,
                        errorMessage = e.message
                    )
                }
            }

            attempt++
            if (attempt <= maxRetries) {
                delay(retryDelay * attempt)
            }
        }

        return lastResponse ?: HttpResponse(
            statusCode = -1,
            headers = emptyMap(),
            body = null,
            errorMessage = "Request failed after $maxRetries retries"
        )
    }

    private suspend fun delay(millis: Long) {
        kotlinx.coroutines.delay(millis)
    }
}
