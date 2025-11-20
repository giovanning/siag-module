package br.com.siag.siagmodulos

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import br.com.siag.siagmodulos.ui.theme.SIAGModulosTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            SIAGModulosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreen(modifier: Modifier) {
    var showWebView by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Aguardando ação...") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showWebView) {
        // Mostra o site para o usuário resolver o Captcha
        SiteDownloadScreen(
            modifier = modifier,
            targetUrl = "https://www.tjsp.jus.br/cac/scp/webRelPublicLstPagPrecatPendentes.aspx",
            onDownloadDetected = { url, cookies ->
                showWebView = false
                status = "Interceptado! Baixando..."

                scope.launch {
                    val hiddenPdf = PdfZipManager.downloadAndExtractPdf(context, url, cookies)

                    if (hiddenPdf != null) {
                        status = "Lendo informações do PDF..."

                        //PDFTextExtractor.extrairUnicoProcesso(hiddenPdf)
                        //3, 1160, 24694
                        val processos =
                            listOf("7000976-24.1992.8.26.0500") //"0029336-19.2014.8.26.0500", "0401878-83.2019.8.26.0500")
                        PDFTextExtractor.extrairMultiplosProcessos(hiddenPdf, processos)

                        status = "Concluído!"

                        // 2. Copia para a pasta Downloads pública (VISÍVEL)
//                        val publicUri = FileSaver.saveFileToPublicDownloads(context, hiddenPdf)
//
//                        if (publicUri != null) {
//                            status = "Sucesso! Salvo em Downloads: ${hiddenPdf.name}"
//
//                            // Opcional: Mostrar um Toast nativo para avisar
//                            Toast.makeText(context, "PDF salvo em Downloads!", Toast.LENGTH_LONG).show()
//                        } else {
//                            status = "Erro ao salvar na pasta pública."
//                        }
                    } else {
                        status = "Erro ao extrair o ZIP."
                    }
                }
            }
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(status)
            if (status == "Aguardando ação...") {
                Button(onClick = { showWebView = true }) {
                    Text("Ir para o Site (Resolver Captcha)")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteDownloadScreen(
    modifier: Modifier,
    targetUrl: String,
    onDownloadDetected: (String, String) -> Unit // (Url, Cookies)
) {
    AndroidView(modifier = modifier, factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true // Necessário para alguns dropdowns

            // Cliente para injetar o Script quando a página carregar
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // --- AQUI A MÁGICA ACONTECE ---
                    // Injeta JS para selecionar o dropdown automaticamente.
                    // Você precisa descobrir o ID do elemento HTML inspecionando o site no Chrome (F12).
                    val script = """
                        // Exemplo: Selecionar a opção com value 'categoria_x' no select com id 'meu_dropdown'
                        var dropdown = document.getElementById('vENT_ID ');
                        if(dropdown) {
                            dropdown.value = '56';
                            // As vezes é necessário disparar o evento de change manualmente
                            dropdown.dispatchEvent(new Event('change'));
                        }
                    """
                    view?.evaluateJavascript(script, null)
                }
            }

            // --- DETECTOR DE DOWNLOAD ---
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                // O site tentou iniciar o download do ZIP.
                // Nós interceptamos aqui!

                // Pegamos os cookies da sessão atual (importante se o site exigir login/sessão)
                val cookies = CookieManager.getInstance().getCookie(url)

                // Chamamos a função de callback para tratar o arquivo
                onDownloadDetected(url, cookies)
            }

            loadUrl(targetUrl)
        }
    }, update = {
        it.loadUrl(targetUrl)
    })
}