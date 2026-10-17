package com.example

import com.example.data.model.ColombianCollectionPoints
import com.example.data.repository.CollectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionRepositoryTest {

    @Test
    fun testAllTenAlcaldiasExist() {
        val points = ColombianCollectionPoints.points
        assertEquals(10, points.size)

        val expectedCities = listOf(
            "Armenia", "Pereira", "Cali", "Medellín", "Ibagué",
            "Manizales", "Bogotá", "Bucaramanga", "Quibdó", "Neiva"
        )

        expectedCities.forEach { city ->
            val found = points.find { it.city.equals(city, ignoreCase = true) }
            assertNotNull("La ciudad $city debe estar presente en los puntos de recolección", found)
            assertTrue("Las coordenadas de $city deben ser válidas", found!!.latitude != 0.0 && found.longitude != 0.0)
            assertTrue("Debe tener instrucciones de entrega", found.instructions.isNotEmpty())
        }
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val repo = CollectionRepository(donationDao = FakeDonationDao())

        // Bogotá (4.5981, -74.0758) to Medellín (6.2442, -75.5736) is approximately ~240 km direct line
        val dist = repo.calculateDistanceKm(4.5981, -74.0758, 6.2442, -75.5736)
        assertTrue("La distancia Bogotá-Medellín debe estar entre 220 y 260 km", dist in 220.0..260.0)
    }

    @Test
    fun testMultiStopRouteOptimization() {
        val repo = CollectionRepository(donationDao = FakeDonationDao())
        val selected = listOf("col_armenia", "col_pereira", "col_manizales")
        val route = repo.optimizeMultiStopRoute("col_armenia", selected)

        assertEquals("Armenia", route.originCity)
        assertEquals(3, route.stops.size)
        assertTrue("La distancia total del Eje Cafetero debe ser razonable (> 50 km)", route.totalDistanceKm > 50.0)
    }

    private class FakeDonationDao : com.example.data.local.DonationDao {
        override fun getAllDonations() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.model.DeviceDonation>())
        override suspend fun getUnsyncedDonations() = emptyList<com.example.data.model.DeviceDonation>()
        override suspend fun insertDonation(donation: com.example.data.model.DeviceDonation): Long = 1L
        override suspend fun markDonationSynced(id: Long) {}
        override fun getDonationCount() = kotlinx.coroutines.flow.flowOf(0)
    }
}
