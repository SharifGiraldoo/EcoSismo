package com.example.data.repository

import com.example.data.local.DonationDao
import com.example.data.model.CollectionPoint
import com.example.data.model.ColombianCollectionPoints
import com.example.data.model.DeviceDonation
import kotlinx.coroutines.flow.Flow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteOptimizationResult(
    val originCity: String,
    val originLat: Double,
    val originLng: Double,
    val stops: List<CollectionPoint>,
    val totalDistanceKm: Double,
    val estimatedDurationMinutes: Int
)

class CollectionRepository(
    private val donationDao: DonationDao
) {
    fun getCollectionPoints(): List<CollectionPoint> {
        return ColombianCollectionPoints.points
    }

    fun getPointById(id: String): CollectionPoint? {
        return ColombianCollectionPoints.points.find { it.id == id }
    }

    val allDonations: Flow<List<DeviceDonation>> = donationDao.getAllDonations()
    val donationCount: Flow<Int> = donationDao.getDonationCount()

    suspend fun registerDonation(
        brand: String,
        model: String,
        condition: String,
        workingSensors: String,
        point: CollectionPoint,
        donorName: String,
        donorContact: String,
        notes: String
    ): Long {
        val donation = DeviceDonation(
            brand = brand,
            model = model,
            condition = condition,
            workingSensors = workingSensors,
            collectionPointId = point.id,
            collectionPointName = point.name,
            city = point.city,
            donorName = donorName,
            donorContact = donorContact,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )
        return donationDao.insertDonation(donation)
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun findNearestPoint(lat: Double, lng: Double): Pair<CollectionPoint, Double>? {
        val points = getCollectionPoints()
        if (points.isEmpty()) return null
        var nearest = points.first()
        var minDistance = calculateDistanceKm(lat, lng, nearest.latitude, nearest.longitude)

        for (p in points) {
            val d = calculateDistanceKm(lat, lng, p.latitude, p.longitude)
            if (d < minDistance) {
                minDistance = d
                nearest = p
            }
        }
        return Pair(nearest, minDistance)
    }

    fun optimizeMultiStopRoute(startPointId: String, selectedPointIds: List<String>): RouteOptimizationResult {
        val allPoints = getCollectionPoints().associateBy { it.id }
        val startPoint = allPoints[startPointId] ?: getCollectionPoints().first()

        val pending = selectedPointIds.filter { it != startPoint.id }.mapNotNull { allPoints[it] }.toMutableList()
        val orderedRoute = mutableListOf(startPoint)
        var totalDist = 0.0

        var current = startPoint
        while (pending.isNotEmpty()) {
            val next = pending.minByOrNull { calculateDistanceKm(current.latitude, current.longitude, it.latitude, it.longitude) }
            if (next != null) {
                totalDist += calculateDistanceKm(current.latitude, current.longitude, next.latitude, next.longitude)
                orderedRoute.add(next)
                pending.remove(next)
                current = next
            }
        }

        val durationMinutes = ((totalDist / 45.0) * 60).toInt() + (orderedRoute.size * 20)

        return RouteOptimizationResult(
            originCity = startPoint.city,
            originLat = startPoint.latitude,
            originLng = startPoint.longitude,
            stops = orderedRoute,
            totalDistanceKm = (totalDist * 10.0).toInt() / 10.0,
            estimatedDurationMinutes = durationMinutes
        )
    }
}

