package com.example.cafeenlinea

import com.example.cafeenlinea.ui.home.model.*
import com.example.cafeenlinea.ui.home.network.CafeteriaService
import com.example.cafeenlinea.ui.home.viewmodel.CafeteriaHomeViewModel
import com.example.cafeenlinea.ui.home.viewmodel.applyFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CafeteriaHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun product(id: Int, name: String, section: String) =
        CafeteriaProduct(id, name, "", "", section, 50.0, "$50.00", "")

    private val response = CafeteriaHomeResponse(
        shop = Shop("Café", "lema", "promo"),
        featured = listOf(FeaturedItem(1, "Burrito de pollo", "Proteína", 35.0, "$35.00", "")),
        sections = listOf(
            CafeteriaSection("Bebidas", "", listOf(product(1, "Café con leche", "Bebidas"))),
            CafeteriaSection("Burritos", "", listOf(product(1, "Burrito de pollo", "Burritos")))
        ),
        combos = listOf(Combo(1, "Combo 1", "Burrito + bebida", 80.0, "$80.00", "")),
        combosNote = "",
        extras = emptyList()
    )

    private val raw = CafeteriaHomeUiState(
        shop = response.shop,
        featured = response.featured,
        sections = response.sections,
        combos = response.combos
    )

    @Test fun `busqueda ignora mayusculas y acentos`() {
        val r = applyFilters(raw, "CAFE con LECHE", null)
        assertEquals(listOf("Café con leche"), r.sections.flatMap { it.products }.map { it.name })
    }

    @Test fun `busqueda filtra destacados y combos`() {
        val r = applyFilters(raw, "combo", null)
        assertEquals(1, r.combos.size)
        assertTrue(r.featured.isEmpty())
    }

    @Test fun `chip de seccion deja solo esa seccion`() {
        val r = applyFilters(raw, "", "Burritos")
        assertEquals(listOf("Burritos"), r.sections.map { it.name })
        assertTrue(r.combos.isEmpty())
    }

    @Test fun `sin coincidencias activa noResults`() {
        assertTrue(applyFilters(raw, "pizza", null).noResults)
    }

    @Test fun `sin filtros no hay noResults`() {
        assertFalse(applyFilters(raw, "", null).noResults)
    }

    @Test fun `viewmodel filtra con servicio falso`() = runTest(dispatcher) {
        val fake = object : CafeteriaService {
            override suspend fun getCafeteriaHome() = response
        }
        val vm = CafeteriaHomeViewModel(fake)
        advanceUntilIdle()
        vm.onSearchQueryChange("burrito")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.sections.size)
    }
}