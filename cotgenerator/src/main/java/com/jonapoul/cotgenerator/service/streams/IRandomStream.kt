package com.jonapoul.cotgenerator.service.streams

interface IRandomStream<T> {
    fun next(): T
}
