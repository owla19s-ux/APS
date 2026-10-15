package com.aps.connection

interface Connection {
    val id: String
    val type: Type

    enum class Type {
        GITHUB,
        AI,
        LOCAL,
        FILE
    }
}
