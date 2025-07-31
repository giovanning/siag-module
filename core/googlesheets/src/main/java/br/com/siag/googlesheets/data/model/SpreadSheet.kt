package br.com.siag.googlesheets.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpreadSheet(
    @Json(name = "range") val range: String,
    @Json(name = "majorDimension") val majorDimension: String,
    @Json(name = "values") val values: List<List<String>>
)
