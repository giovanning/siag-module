package br.com.siag.googlesheets.data.mapper

import br.com.siag.googlesheets.data.model.Planilha
import br.com.siag.googlesheets.data.model.SpreadSheet

fun SpreadSheet.toPlanilhas(): List<Planilha> {
    if (!isValoresPlanilhasValidos(this.values)) {
        return emptyList()
    }

    return this.values
        .drop(1)
        .map { informacao ->
            Planilha(
                topico = informacao.getOrElse(0) { "" },
                valor = informacao.getOrElse(1) { "" },
                observacao = informacao.getOrElse(2) { "" }
            )
        }
}

private fun isValoresPlanilhasValidos(spreadSheet: List<List<String>>) = spreadSheet.isNotEmpty()