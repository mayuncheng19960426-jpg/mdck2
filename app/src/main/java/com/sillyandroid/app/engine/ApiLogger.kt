package com.sillyandroid.app.engine

import com.sillyandroid.app.SillyApp
import com.sillyandroid.app.data.entity.ApiLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * API 调用日志记录器。
 */
object ApiLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun logRequest(endpoint: String, model: String, requestJson: String, tokenCount: Int) {
        scope.launch {
            try {
                SillyApp.instance.database.apiLogDao().insert(
                    ApiLogEntity(
                        direction = "request",
                        endpoint = endpoint,
                        model = model,
                        requestJson = requestJson,
                        tokenCount = tokenCount
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun logResponse(responseText: String, durationMs: Long) {
        scope.launch {
            try {
                // Update the most recent request log with response
                val recent = SillyApp.instance.database.apiLogDao().getRecent(1)
                if (recent.isNotEmpty()) {
                    SillyApp.instance.database.apiLogDao().insert(
                        ApiLogEntity(
                            direction = "response",
                            endpoint = recent.first().endpoint,
                            model = recent.first().model,
                            responseText = responseText,
                            durationMs = durationMs
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun logError(errorMessage: String) {
        scope.launch {
            try {
                val recent = SillyApp.instance.database.apiLogDao().getRecent(1)
                SillyApp.instance.database.apiLogDao().insert(
                    ApiLogEntity(
                        direction = "error",
                        endpoint = recent.firstOrNull()?.endpoint ?: "",
                        model = recent.firstOrNull()?.model ?: "",
                        errorMessage = errorMessage
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
