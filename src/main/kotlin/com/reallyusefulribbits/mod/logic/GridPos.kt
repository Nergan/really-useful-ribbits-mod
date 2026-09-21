package com.reallyusefulribbits.mod.logic

data class GridPos(val x: Int, val y: Int, val z: Int) {
    fun distSq(other: GridPos): Int {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    fun neighbors6(): List<GridPos> = listOf(
        copy(x = x + 1),
        copy(x = x - 1),
        copy(y = y + 1),
        copy(y = y - 1),
        copy(z = z + 1),
        copy(z = z - 1),
    )
}
