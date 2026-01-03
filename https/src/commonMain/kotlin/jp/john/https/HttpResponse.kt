package jp.john.https


data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String?,
    val isSuccessful: Boolean = statusCode in 200..299,
    val errorMessage: String? = null
)
