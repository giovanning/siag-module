package br.com.siag.siagmodulos

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

object PdfZipManager {

    // Função suspensa para rodar fora da thread principal
    suspend fun downloadAndExtractPdf(
        context: Context,
        pdfUrl: String,
        cookies: String? = null // Novo parâmetro
    ): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Definir arquivos temporários no cache do app
            val zipFile = File(context.cacheDir, "temp_download.zip")
            val outputDir = File(context.cacheDir, "extracted_pdfs")

            if (!outputDir.exists()) outputDir.mkdirs()

            // 2. DOWNLOAD: Baixar o arquivo ZIP da URL
            val url = URL(pdfUrl)
            val connection = url.openConnection()

            // INSERIR O COOKIE NO CABEÇALHO
            if (cookies != null) {
                connection.setRequestProperty("Cookie", cookies)
            }
            // As vezes é bom fingir ser um navegador real
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")

            connection.connect()

            // Salvar o stream da internet no arquivo zip local
            connection.getInputStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 3. UNZIP: Procurar e extrair o PDF
            var extractedPdfFile: File? = null

            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zipInputStream ->
                var entry = zipInputStream.nextEntry

                while (entry != null) {
                    val fileName = entry.name

                    // Verifica se é um PDF e ignora diretórios (MACOSX, etc)
                    if (!entry.isDirectory && fileName.endsWith(".pdf", ignoreCase = true)) {

                        // Cria o arquivo final do PDF
                        val targetFile = File(outputDir, fileName.substringAfterLast("/"))

                        FileOutputStream(targetFile).use { fileOutput ->
                            zipInputStream.copyTo(fileOutput)
                        }

                        extractedPdfFile = targetFile
                        // Se você só precisa do primeiro PDF encontrado, pode parar aqui
                        break
                    }
                    entry = zipInputStream.nextEntry
                }
            }

            // Limpeza opcional: apagar o zip depois de extrair
            if (zipFile.exists()) zipFile.delete()

            // Retorna o arquivo PDF (ou null se falhou/não achou)
            return@withContext extractedPdfFile

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}