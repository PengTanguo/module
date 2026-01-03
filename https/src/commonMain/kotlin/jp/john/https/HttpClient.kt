package jp.john.https



/**
 * 跨平台 HTTP 客户端接口
 */
expect class PlatformHttpClient() {
    suspend fun execute(request: HttpRequest): HttpResponse
}

/**
 * HTTP 客户端构建器
 */
class HttpClient private constructor(
    private val interceptors: List<Interceptor>,
    private val defaultHeaders: Map<String, String>,
    private val timeout: Long
) {
    private val platformClient = PlatformHttpClient()

    /**
     * GET 请求
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse {
        return request(
            HttpRequest(
                url = url,
                method = HttpMethod.GET,
                headers = mergeHeaders(headers),
                queryParams = queryParams,
                timeout = timeout
            )
        )
    }

    /**
     * POST 请求
     */
    suspend fun post(
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse {
        return request(
            HttpRequest(
                url = url,
                method = HttpMethod.POST,
                headers = mergeHeaders(headers),
                body = body,
                queryParams = queryParams,
                timeout = timeout
            )
        )
    }

    /**
     * PUT 请求
     */
    suspend fun put(
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse {
        return request(
            HttpRequest(
                url = url,
                method = HttpMethod.PUT,
                headers = mergeHeaders(headers),
                body = body,
                queryParams = queryParams,
                timeout = timeout
            )
        )
    }

    /**
     * DELETE 请求
     */
    suspend fun delete(
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse {
        return request(
            HttpRequest(
                url = url,
                method = HttpMethod.DELETE,
                headers = mergeHeaders(headers),
                body = body,
                queryParams = queryParams,
                timeout = timeout
            )
        )
    }

    /**
     * PATCH 请求
     */
    suspend fun patch(
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): HttpResponse {
        return request(
            HttpRequest(
                url = url,
                method = HttpMethod.PATCH,
                headers = mergeHeaders(headers),
                body = body,
                queryParams = queryParams,
                timeout = timeout
            )
        )
    }

    /**
     * 通用请求方法
     */
    private suspend fun request(request: HttpRequest): HttpResponse {
        val chain = RealInterceptorChain(
            interceptors = interceptors,
            index = 0,
            request = request,
            call = { platformClient.execute(it) }
        )
        return chain.proceed(request)
    }

    private fun mergeHeaders(headers: Map<String, String>): Map<String, String> {
        return defaultHeaders + headers
    }

    /**
     * 构建器
     */
    class Builder {
        private val interceptors = mutableListOf<Interceptor>()
        private val defaultHeaders = mutableMapOf<String, String>()
        private var timeout: Long = 30000L

        fun addInterceptor(interceptor: Interceptor) = apply {
            interceptors.add(interceptor)
        }

        fun addHeader(key: String, value: String) = apply {
            defaultHeaders[key] = value
        }

        fun addHeaders(headers: Map<String, String>) = apply {
            defaultHeaders.putAll(headers)
        }

        fun setTimeout(timeout: Long) = apply {
            this.timeout = timeout
        }

        fun build(): HttpClient {
            return HttpClient(
                interceptors = interceptors.toList(),
                defaultHeaders = defaultHeaders.toMap(),
                timeout = timeout
            )
        }
    }
}
