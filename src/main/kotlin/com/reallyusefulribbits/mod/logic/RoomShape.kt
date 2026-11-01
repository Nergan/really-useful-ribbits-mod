package com.reallyusefulribbits.mod.logic

/** Комната 16×16×16 вокруг ног игрока: пол на блок ниже ног, стены и потолок в один блок. */
data class RoomShape(val minX: Int, val minY: Int, val minZ: Int) {
    val maxX: Int get() = minX + SIZE - 1
    val maxY: Int get() = minY + SIZE - 1
    val maxZ: Int get() = minZ + SIZE - 1
    val centerX: Int get() = minX + SIZE / 2 - 1
    val centerZ: Int get() = minZ + SIZE / 2 - 1

    fun isShell(x: Int, y: Int, z: Int): Boolean =
        x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ

    inline fun forEach(action: (Int, Int, Int) -> Unit) {
        for (x in minX..maxX) for (y in minY..maxY) for (z in minZ..maxZ) action(x, y, z)
    }

    fun corners(): List<Pair<Int, Int>> = listOf(minX to minZ, minX to maxZ, maxX to minZ, maxX to maxZ)

    companion object {
        const val SIZE: Int = 16

        fun around(feetX: Int, feetY: Int, feetZ: Int): RoomShape =
            RoomShape(feetX - (SIZE / 2 - 1), feetY - 1, feetZ - (SIZE / 2 - 1))
    }
}
