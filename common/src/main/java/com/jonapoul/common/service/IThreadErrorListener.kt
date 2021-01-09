package com.jonapoul.common.service;

interface IThreadErrorListener {
    fun onThreadError(throwable: Throwable);
}
