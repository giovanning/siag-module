package br.com.siag.siagmodulos

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

data class Processo(
    val ordemPagamento: String,
    val numeroProcesso: String,
    val suspenso: Boolean,
    val devedora: String
)

object PDFTextExtractor {

    suspend fun extrairUnicoProcesso(file: File) = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            document = PDDocument.load(file)
            Log.d("PDFTextExtractor", "Documento carregado. Total de páginas: ${document.numberOfPages}")

            val stripper = PDFTextStripper()
            var blocoEncontrado: String? = null
            val idAlvo = "7001381-94.1991.8.26.0500"

            for (i in 1..document.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i

                val textoDaPagina = stripper.getText(document)

                if (textoDaPagina.contains("Ordem de Pagamento") && textoDaPagina.contains(idAlvo)) {
                    val blocosNaPagina = textoDaPagina.split("Ordem de")
                    blocoEncontrado = blocosNaPagina.find { it.contains(idAlvo) }
                    break
                }
            }

            if (blocoEncontrado != null) {
                val valor = extrairValor(blocoEncontrado, "Nº", "Devedora:")
                Log.d("PDFTextExtractor", "Valor final extraído: $valor")
            } else {
                Log.d("PDFTextExtractor", "ID '$idAlvo' não encontrado em nenhuma página do documento.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext
        } finally {
            document?.close() // Importante fechar para não vazar memória
        }
    }

    suspend fun extrairMultiplosProcessos(file: File, idsAlvo: List<String>) = withContext(Dispatchers.IO) {
        val processosEncontrados = mutableListOf<Processo>()
        // Cria uma cópia mutável para removermos os IDs que já foram encontrados.
        val idsRestantes = idsAlvo.toMutableSet()
        var document: PDDocument? = null

        if (idsRestantes.isEmpty()) return@withContext

        try {
            document = PDDocument.load(file)
            Log.d("PDFTextExtractor", "Documento carregado. Total de páginas: ${document.numberOfPages}")

            val stripper = PDFTextStripper()

            var pageCount = 1

            for (i in 1..document.numberOfPages) {
                Log.d("PDFTextExtractor", "Página atual: $pageCount")
                // Otimização: Se todos os IDs já foram achados, pare de ler o arquivo.
                if (idsRestantes.isEmpty()) {
                    Log.d("PDFTextExtractor", "Todos os alvos encontrados. Interrompendo a busca.")
                    break
                }

                stripper.startPage = i
                stripper.endPage = i
                val textoDaPagina = stripper.getText(document)

                val idsNestaPagina = idsRestantes.filter { id -> textoDaPagina.contains(id) }

                pageCount +=1

                Log.d("PDFTextExtractor", "Lista restante: $idsRestantes")

                if (idsNestaPagina.isNotEmpty()) {
                    val blocosNaPagina = textoDaPagina.split("Ordem de")

                    // 3. Itera sobre os IDs encontrados *nesta página específica*
                    for (idEncontrado in idsNestaPagina) {
                        blocosNaPagina.find { it.contains(idEncontrado) }
                            ?.let { bloco ->
                                // Adiciona o prefixo para reconstruir o bloco completo
                                val blocoCompleto = "Ordem de$bloco"
                                construirProcessoDoBloco(blocoCompleto, idEncontrado)?.let { processo ->
                                    idsRestantes.remove(idEncontrado)
                                }
                            }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Retorna o que foi encontrado até o momento do erro
            return@withContext
        } finally {
            document?.close()
        }

        Log.d("PDFTextExtractor", "Busca finalizada. Total de processos encontrados: ${processosEncontrados.size}")
        return@withContext
    }

    /**
     * Constrói um objeto Processo a partir de um bloco de texto extraído.
     */
    private fun construirProcessoDoBloco(bloco: String, idProcesso: String) {
        try {
            val ordemPagamento = extrairProximaLinha(bloco, "Ordem Orçamentária:")

            val suspenso = "\\s*Suspenso\\?\\s*(?i)S".toRegex().containsMatchIn(bloco)

            Log.d("PDFTextExtractor", bloco)
            Log.d("PDFTextExtractor", "Valor final extraído na ordem de pagamento: $ordemPagamento e suspenso: $suspenso")
        } catch (e: Exception) {
            Log.e("PDFTextExtractor", "Falha ao construir processo para o ID $idProcesso", e)
        }
    }

    private fun extrairValor(texto: String, inicio: String, fim: String): String {
        val regex = "(?s)${Pattern.quote(inicio)}\\s*(.*?)${Pattern.quote(fim)}".toRegex()
        return regex.find(texto)?.groupValues?.get(1)?.trim() ?: "N/D"
    }

    private fun extrairProximaLinha(texto: String, inicio: String): String {
        // Regex explicado:
        // ${Pattern.quote(inicio)} -> Encontra o texto de início literal.
        // (?:\\s*\\R\\s*)+ -> Encontra uma ou mais sequências de "espaços/quebra de linha/espaços".
        //                     O `\\R` é um metacaractere para qualquer tipo de quebra de linha (CR, LF, CRLF).
        //                     O `(?: ... )` é um grupo de não captura, apenas para agrupar a lógica.
        // ([^\\r\\n]+) -> Captura (esse é o grupo 1) uma sequência de um ou mais caracteres
        //                que NÃO são quebras de linha. Ou seja, captura o conteúdo da linha.
        val regex = "${Pattern.quote(inicio)}(?:\\s*\\R\\s*)+([^\\r\\n]+)".toRegex()
        return regex.find(texto)?.groupValues?.get(1)?.trim() ?: "N/D"
    }
}