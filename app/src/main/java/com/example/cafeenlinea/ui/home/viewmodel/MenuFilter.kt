package com.example.cafeenlinea.ui.home.viewmodel

import com.example.cafeenlinea.ui.home.model.CafeteriaHomeUiState
import java.text.Normalizer

/** Minúsculas y sin acentos: "Café" -> "cafe". */
internal fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .trim()

/**
 * Aplica búsqueda y sección sobre el menú completo (`raw`).
 * Si hay una sección seleccionada, los destacados y combos se ocultan
 * porque no pertenecen a ninguna sección.
 */
internal fun applyFilters(
    raw: CafeteriaHomeUiState,
    query: String,
    selectedSection: String?
): CafeteriaHomeUiState {
    val q = query.normalizeForSearch()
    fun String.hit() = q.isEmpty() || normalizeForSearch().contains(q)

    val sections = raw.sections
        .filter { selectedSection == null || it.name == selectedSection }
        .map { s ->
            s.copy(products = s.products.filter { it.name.hit() || it.description.hit() || it.badge.hit() })
        }
        .filter { it.products.isNotEmpty() }

    val featured = if (selectedSection == null)
        raw.featured.filter { it.name.hit() || it.badge.hit() } else emptyList()

    val combos = if (selectedSection == null)
        raw.combos.filter { it.name.hit() || it.includes.hit() } else emptyList()

    val filtering = q.isNotEmpty() || selectedSection != null
    val empty = sections.isEmpty() && featured.isEmpty() && combos.isEmpty()

    return raw.copy(
        sections = sections,
        featured = featured,
        combos = combos,
        searchQuery = query,
        selectedSection = selectedSection,
        sectionNames = raw.sections.map { it.name },
        noResults = filtering && empty && !raw.isLoading && raw.shop != null
    )
}