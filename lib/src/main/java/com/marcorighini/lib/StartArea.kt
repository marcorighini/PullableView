package com.marcorighini.lib

data class StartArea(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int){
    fun inBounds(x: Int, y: Int) = x > minX && x <= maxX && y > minY && y <= maxY
}