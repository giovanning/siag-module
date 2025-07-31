package br.com.siag.googlesheets.data.handle_response

sealed class OutCome<out T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : OutCome<T>(data)
    class Error<T>(message: String?) : OutCome<T>(message = message)
    object Loading : OutCome<Nothing>()
}