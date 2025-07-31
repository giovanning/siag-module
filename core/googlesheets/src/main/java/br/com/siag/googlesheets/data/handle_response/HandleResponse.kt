package br.com.siag.googlesheets.data.handle_response

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T,
): OutCome<T> {
    return withContext(dispatcher) {
        try {
            OutCome.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> OutCome.Error("No internet connection")
                is HttpException -> {
                    OutCome.Error(message = "Server error occurred. Code: ${throwable.code()} - ${throwable.message()}")
                }

                else -> OutCome.Error(throwable.message ?: "Unexpected error occurred")
            }
        }
    }
}