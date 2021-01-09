package com.jonapoul.cotgenerator.service.streams

import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sqrt

/**
 * Used to generate distance of an icon to the centre-point within a circle of radius R
 */
class RadialDistanceRandomStream(random: Random, maxDistance: Double) : IRandomStream<Double> {
    private val weightedElements = ArrayList<Double>()
    private val indexStream = IntRandomStream(
            random = random,
            min = 0,
            max = NUM_WEIGHTED_ELEMENTS - 1
    )

    init {
        for (i in 0 until NUM_WEIGHTED_ELEMENTS) {
            val r = i.toDouble() / NUM_WEIGHTED_ELEMENTS.toDouble()
            weightedElements.add(
                    sqrt(r) * maxDistance
            )
        }
    }

    override fun next(): Double {
        return weightedElements[indexStream.next()]
    }

    private companion object {
        const val NUM_WEIGHTED_ELEMENTS = 1000
    }
}
