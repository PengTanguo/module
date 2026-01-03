package jp.john.https


data class HttpRequest(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val queryParams: Map<String, String> = emptyMap(),
    val timeout: Long = 30000L // 默认30秒
) {
    fun buildUrl(): String {
        if (queryParams.isEmpty()) return url

        val params = queryParams.entries.joinToString("&") { (key, value) ->
            "${encodeURIComponent(key)}=${encodeURIComponent(value)}"
        }
        return if (url.contains("?")) "$url&$params" else "$url?$params"
    }

    private fun encodeURIComponent(str: String): String {
        return str.replace(" ", "%20")
            .replace("!", "%21")
            .replace("#", "%23")
            .replace("$", "%24")
            .replace("&", "%26")
            .replace("'", "%27")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("*", "%2A")
            .replace("+", "%2B")
            .replace(",", "%2C")
            .replace("/", "%2F")
            .replace(":", "%3A")
            .replace(";", "%3B")
            .replace("=", "%3D")
            .replace("?", "%3F")
            .replace("@", "%40")
            .replace("[", "%5B")
            .replace("]", "%5D")
    }
}
