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

    fun neighborsFarm(): List<GridPos> {
        val out = ArrayList<GridPos>(10)
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                out += copy(x = x + dx, z = z + dz)
            }
        }
        out += copy(y = y + 1)
        out += copy(y = y - 1)
        return out
    }
}
