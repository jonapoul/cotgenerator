package com.jonapoul.cotgenerator.service.streams

import java.util.*

class IntRandomStream(private val random: Random, private val min: Int, private val max: Int) : IRandomStream<Int> {
    override fun next(): Int {
        return min + random.nextInt(max - min)
    }
}
