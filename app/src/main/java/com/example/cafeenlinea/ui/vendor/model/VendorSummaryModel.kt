package com.example.cafeenlinea.ui.vendor.model

import com.example.cafeenlinea.data.remote.Pedido
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Cuántos productos se muestran en "Lo más vendido". */
const val MAS_VENDIDOS_MAXIMO = 5

data class PedidosPorHora(val hora: Int, val pedidos: Int)
data class MasVendido(val nombre: String, val unidades: Int, val ingresos: Double)

/**
 * Resumen del vendedor calculado con los pedidos de su cafetería. El servidor
 * tiene reportes, pero son del JEFE; el vendedor solo puede leer los pedidos.
 */
data class ResumenVendedor(
    /** Pedidos de hoy sin contar cancelados. */
    val pedidosHoy: Int = 0,
    val entregadosHoy: Int = 0,
    /** Suma de lo entregado hoy. */
    val ventasHoy: Double = 0.0,
    val moneda: String = MONEDA_POR_DEFECTO,
    /** De la primera a la última hora con pedidos hoy, incluidas las horas sin pedidos. */
    val porHora: List<PedidosPorHora> = emptyList(),
    /** Histórico de pedidos entregados, por unidades. */
    val masVendidos: List<MasVendido> = emptyList(),
    /** Minutos promedio de "en preparación" a "listo". */
    val preparacionPromedioMin: Double? = null,
    /** Minutos promedio desde que se hizo el pedido hasta que se entregó. */
    val entregaPromedioMin: Double? = null,
    val pedidosMedidos: Int = 0
) {
    val horaPico: PedidosPorHora? get() = porHora.filter { it.pedidos > 0 }.maxByOrNull { it.pedidos }
}

/** Fecha ISO del servidor ("2026-11-24T18:25:00.000Z") a milisegundos, o null si no se puede leer. */
fun isoAMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    for (patron in listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX")) {
        val fecha = runCatching {
            SimpleDateFormat(patron, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso)
        }.getOrNull()
        if (fecha != null) return fecha.time
    }
    return null
}

private fun Pedido.esEstado(clave: String) = estado.trim().equals(clave, ignoreCase = true)

/** Fecha en que el pedido pasó a [estado], según su historial. */
private fun Pedido.fechaDe(estado: String): Long? =
    historial.orEmpty().lastOrNull { it.estado.equals(estado, ignoreCase = true) }?.let { isoAMillis(it.fecha) }

/**
 * @param ahora Milisegundos de "ahora" (para saber qué es hoy).
 * @param zona Zona horaria del teléfono: "hoy" y las horas son las locales.
 */
fun calcularResumen(
    pedidos: List<Pedido>,
    ahora: Long = System.currentTimeMillis(),
    zona: TimeZone = TimeZone.getDefault()
): ResumenVendedor {
    val calendario = Calendar.getInstance(zona)
    fun diaYHora(millis: Long): Pair<Int, Int> {
        calendario.timeInMillis = millis
        val dia = calendario.get(Calendar.YEAR) * 1000 + calendario.get(Calendar.DAY_OF_YEAR)
        return dia to calendario.get(Calendar.HOUR_OF_DAY)
    }
    val hoy = diaYHora(ahora).first

    val deHoy = pedidos.mapNotNull { pedido ->
        val creado = isoAMillis(pedido.creadoEn) ?: return@mapNotNull null
        val (dia, hora) = diaYHora(creado)
        if (dia == hoy && !pedido.esEstado(FlujoPedido.CANCELADO)) pedido to hora else null
    }
    val entregadosHoy = deHoy.map { it.first }.filter { it.esEstado(FlujoPedido.ENTREGADO) }

    val conteoPorHora = deHoy.groupingBy { it.second }.eachCount()
    val porHora = if (conteoPorHora.isEmpty()) emptyList() else
        (conteoPorHora.keys.min()..conteoPorHora.keys.max()).map { PedidosPorHora(it, conteoPorHora[it] ?: 0) }

    val entregados = pedidos.filter { it.esEstado(FlujoPedido.ENTREGADO) }
    val masVendidos = entregados
        .flatMap { it.items }
        .groupBy { it.nombreProducto }
        .map { (nombre, items) ->
            MasVendido(
                nombre = nombre,
                unidades = items.sumOf { it.cantidad },
                ingresos = items.sumOf { (it.precioUnitario?.monto ?: 0.0) * it.cantidad }
            )
        }
        .sortedWith(compareByDescending<MasVendido> { it.unidades }.thenByDescending { it.ingresos })
        .take(MAS_VENDIDOS_MAXIMO)

    val minuto = 60_000.0
    val preparaciones = entregados.mapNotNull { p ->
        val inicio = p.fechaDe(FlujoPedido.EN_PREPARACION) ?: return@mapNotNull null
        val listo = p.fechaDe(FlujoPedido.LISTO) ?: return@mapNotNull null
        (listo - inicio).takeIf { it >= 0 }?.div(minuto)
    }
    val entregas = entregados.mapNotNull { p ->
        val creado = isoAMillis(p.creadoEn) ?: return@mapNotNull null
        val entregado = p.fechaDe(FlujoPedido.ENTREGADO) ?: return@mapNotNull null
        (entregado - creado).takeIf { it >= 0 }?.div(minuto)
    }

    return ResumenVendedor(
        pedidosHoy = deHoy.size,
        entregadosHoy = entregadosHoy.size,
        ventasHoy = entregadosHoy.sumOf { it.total?.monto ?: 0.0 },
        moneda = pedidos.firstNotNullOfOrNull { it.total?.moneda } ?: MONEDA_POR_DEFECTO,
        porHora = porHora,
        masVendidos = masVendidos,
        preparacionPromedioMin = preparaciones.takeIf { it.isNotEmpty() }?.average(),
        entregaPromedioMin = entregas.takeIf { it.isNotEmpty() }?.average(),
        pedidosMedidos = entregas.size
    )
}

/** Estado de la pestaña Resumen. */
data class VendorSummaryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val resumen: ResumenVendedor = ResumenVendedor()
)
