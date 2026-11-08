package com.example.cafeenlinea.ui.profile.view

import android.util.Log
import androidx.compose.ui.res.stringResource
import com.example.cafeenlinea.R
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cafeenlinea.common.Config
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private suspend fun descargarTerminos(): String = withContext(Dispatchers.IO) {
    val conexion = URL(Config.URL_SERVIDOR).openConnection() as HttpURLConnection
    try {
        conexion.requestMethod = "POST"
        conexion.setRequestProperty("Content-Type", "application/json")
        conexion.connectTimeout = 8000
        conexion.readTimeout = 8000
        conexion.doOutput = true

        val cuerpo = JSONObject().put("query", "{ terminos }").toString()
        conexion.outputStream.use { it.write(cuerpo.toByteArray()) }

        val codigo = conexion.responseCode
        val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val respuesta = flujo.bufferedReader().readText()
        Log.d("Terminos", "HTTP $codigo -> $respuesta")

        if (codigo !in 200..299) {
            throw Exception("HTTP $codigo: ${respuesta.take(200)}")
        }
        val json = JSONObject(respuesta)
        if (!json.has("data") || json.isNull("data")) {
            throw Exception("Respuesta sin data: ${respuesta.take(200)}")
        }
        json.getJSONObject("data").getString("terminos")
    } finally {
        conexion.disconnect()
    }
}

@Composable
private fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val markwon = remember { Markwon.create(contexto) }
    val colorTexto = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextView(ctx).apply { textSize = 14f } },
        update = { textView ->
            textView.setTextColor(colorTexto)
            markwon.setMarkdown(textView, markdown)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsView(onBack: () -> Unit) {
    var texto by remember { mutableStateOf<String?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var intento by remember { mutableIntStateOf(0) }

    LaunchedEffect(intento) {
        texto = null
        mensajeError = null
        try {
            texto = descargarTerminos()
        } catch (e: Exception) {
            Log.e("Terminos", "Falló la descarga", e)
            mensajeError = "${e.javaClass.simpleName}: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_terms)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                texto != null -> MarkdownText(
                    markdown = texto!!,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                )

                mensajeError != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("No se pudieron cargar los términos.")
                    Text("Dirección: ${Config.URL_SERVIDOR}", style = MaterialTheme.typography.bodySmall)
                    Text(mensajeError!!, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { intento++ }) { Text("Reintentar") }
                }

                else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}