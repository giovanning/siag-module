package br.com.siag.siagmodulos

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream

object FileSaver {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveFileToPublicDownloads(context: Context, sourceFile: File): Uri? {
        val contentResolver = context.contentResolver

        // Configura os detalhes do arquivo que vai ser criado na pasta pública
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name) // Mantém o nome original
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            // Para Android 10+ define a pasta relativa (Downloads)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // Cria a entrada no sistema de arquivos público
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        return if (uri != null) {
            try {
                // Abre o fluxo de escrita para o novo arquivo público
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Abre o arquivo que estava no cache (sourceFile)
                    FileInputStream(sourceFile).use { inputStream ->
                        // Copia os bytes de um para o outro
                        inputStream.copyTo(outputStream)
                    }
                }
                uri // Retorna o endereço do arquivo salvo com sucesso
            } catch (e: Exception) {
                e.printStackTrace()
                // Se der erro, tenta apagar o arquivo vazio que foi criado
                contentResolver.delete(uri, null, null)
                null
            }
        } else {
            null
        }
    }
}