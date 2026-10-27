package com.deedeedev.ytreader.data

import android.content.Context
import com.deedeedev.ytreader.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AiChatMessage(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val endpointBaseUrl: String,
    val apiKey: String,
    val model: String,
    val messages: List<AiChatMessage>
)

class AiChatRepository(
    private val context: Context,
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) {

    suspend fun chat(request: AiChatRequest): String = withContext(Dispatchers.IO) {
        val endpoint = buildEndpoint(request.endpointBaseUrl)
        val payload = ChatCompletionsRequest(
            model = request.model,
            messages = request.messages,
            temperature = 0.4
        )

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${request.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val promptLength = request.messages.sumOf { it.content.length }
        val requestClient = client.newBuilder()
            .readTimeout(
                AiCleaningRepository.computeReadTimeout(promptLength),
                TimeUnit.SECONDS
            )
            .callTimeout(
                AiCleaningRepository.computeCallTimeout(promptLength),
                TimeUnit.SECONDS
            )
            .build()

        executeCancellable(requestClient, httpRequest).use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_chat_request_failed, response.code),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_model, request.model),
                            context.getString(R.string.ai_cleaning_log_http_status, response.code),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }

            val answer = parseResponseText(body)?.trim().orEmpty()
            if (answer.isEmpty()) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_chat_missing_response_text),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_model, request.model),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }

            answer
        }
    }

    fun toFailure(request: AiChatRequest, throwable: Throwable): AiCleaningFailure {
        if (throwable is AiCleaningException) {
            return throwable.failure
        }
        val endpoint = runCatching { buildEndpoint(request.endpointBaseUrl) }.getOrNull()
        return AiCleaningFailureFactory.fromThrowable(
            resources = context.resources,
            throwable = throwable,
            endpoint = endpoint,
            model = request.model,
            fallbackSummary = context.getString(R.string.ai_chat_failed),
            timeoutSummary = context.getString(R.string.ai_chat_timed_out)
        )
    }

    internal fun buildEndpoint(baseOrFullUrl: String): String {
        val normalized = baseOrFullUrl.trim().trimEnd('/')
        return if (normalized.endsWith(CHAT_COMPLETIONS_PATH)) {
            normalized
        } else {
            normalized + CHAT_COMPLETIONS_PATH
        }
    }

    internal fun buildModelsEndpoint(baseOrFullUrl: String): String {
        val normalized = baseOrFullUrl.trim().trimEnd('/')
        return if (normalized.endsWith(MODELS_PATH)) {
            normalized
        } else {
            normalized + MODELS_PATH
        }
    }

    internal fun parseModelIds(responseBody: String): List<String> {
        return try {
            val response = gson.fromJson(responseBody, ModelsResponse::class.java) ?: return emptyList()
            response.data
                .mapNotNull { it.id }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listModels(
        endpointBaseUrl: String,
        apiKey: String
    ): List<String> = withContext(Dispatchers.IO) {
        val endpoint = buildModelsEndpoint(endpointBaseUrl)
        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        val requestClient = client.newBuilder()
            .connectTimeout(MODELS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(MODELS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(MODELS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        executeCancellable(requestClient, httpRequest).use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_cleaning_request_failed, response.code),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_http_status, response.code),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }

            val models = parseModelIds(body)
            if (models.isEmpty()) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_cleaning_missing_response_text),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }
            models
        }
    }

    suspend fun pingModel(
        endpointBaseUrl: String,
        apiKey: String,
        model: String
    ): Long = withContext(Dispatchers.IO) {
        val endpoint = buildEndpoint(endpointBaseUrl)
        val payload = ChatCompletionsRequest(
            model = model,
            messages = listOf(AiChatMessage(role = "user", content = PING_MESSAGE)),
            temperature = 0.0,
            maxTokens = PING_MAX_TOKENS
        )

        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val requestClient = client.newBuilder()
            .connectTimeout(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        val startedAt = System.currentTimeMillis()
        executeCancellable(requestClient, httpRequest).use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_chat_request_failed, response.code),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_model, model),
                            context.getString(R.string.ai_cleaning_log_http_status, response.code),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }
            val answer = parseResponseText(body)?.trim().orEmpty()
            if (answer.isEmpty()) {
                throw AiCleaningException(
                    AiCleaningFailureFactory.buildFailure(
                        resources = context.resources,
                        summary = context.getString(R.string.ai_chat_missing_response_text),
                        lines = listOf(
                            context.getString(R.string.ai_cleaning_log_endpoint, endpoint),
                            context.getString(R.string.ai_cleaning_log_model, model),
                            context.getString(R.string.ai_cleaning_log_response_body),
                            AiCleaningFailureFactory.sanitizeLog(
                                body.ifBlank { context.getString(R.string.ai_cleaning_log_empty) },
                                context.resources
                            )
                        )
                    )
                )
            }
            System.currentTimeMillis() - startedAt
        }
    }

    internal fun parseResponseText(responseBody: String): String? {
        return try {
            val response = gson.fromJson(responseBody, ChatCompletionsResponse::class.java)
                ?: return null
            response.choices
                .firstOrNull()
                ?.message
                ?.content
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun executeCancellable(
        requestClient: OkHttpClient,
        request: Request
    ): Response =
        suspendCancellableCoroutine { continuation ->
            val call = requestClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            })
        }

    private data class ChatCompletionsRequest(
        val model: String,
        val messages: List<AiChatMessage>,
        val temperature: Double,
        @com.google.gson.annotations.SerializedName("max_tokens")
        val maxTokens: Int? = null
    )

    private data class ModelsResponse(
        val data: List<ModelsResponseItem> = emptyList()
    )

    private data class ModelsResponseItem(
        val id: String? = null
    )

    private data class ChatCompletionsResponse(
        val choices: List<ChatChoice> = emptyList()
    )

    private data class ChatChoice(
        val message: ChatMessageContent? = null
    )

    private data class ChatMessageContent(
        val content: String? = null
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val CHAT_COMPLETIONS_PATH = "/chat/completions"
        private const val MODELS_PATH = "/models"
        private const val MODELS_TIMEOUT_SECONDS = 30L
        private const val PING_TIMEOUT_SECONDS = 30L
        private const val PING_MAX_TOKENS = 16
        private const val PING_MESSAGE = "Reply with OK."
    }
}
