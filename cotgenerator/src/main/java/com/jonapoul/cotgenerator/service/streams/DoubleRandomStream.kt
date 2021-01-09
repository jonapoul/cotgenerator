package com.jonapoul.cotgenerator.service.streams

import java.util.*

class DoubleRandomStream(private val random: Random, private val min: Double, private val max: Double) : IRandomStream<Double> {
    override fun next(): Double {
        return min + (max - min) * random.nextDouble()
    }
}
