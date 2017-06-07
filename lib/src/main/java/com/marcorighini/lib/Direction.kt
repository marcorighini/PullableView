package com.marcorighini.lib

enum class Direction(val value: Int){
    UP(0), DOWN(1), BOTH(2);

    companion object {
        fun from(findValue: Int): Direction = Direction.values().first { it.value == findValue }
    }

    fun upEnabled() = this == UP || this == BOTH
    fun downEnabled() = this == UP || this == BOTH
}