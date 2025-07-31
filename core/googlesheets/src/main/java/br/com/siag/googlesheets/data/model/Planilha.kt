package br.com.siag.googlesheets.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Planilha(
    val topico: String = "",
    val valor: String = "",
    val observacao: String = ""
): Parcelable