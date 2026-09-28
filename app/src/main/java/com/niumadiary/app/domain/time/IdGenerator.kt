package com.niumadiary.app.domain.time

fun interface IdGenerator {
    fun newId(): String
}
