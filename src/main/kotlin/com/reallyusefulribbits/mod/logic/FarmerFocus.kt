package com.reallyusefulribbits.mod.logic

/**
 * Сколько грядок фермер реально проверяет за тик.
 * Ближние смотрит всегда, дальние — небольшим окном, иначе большой огород читает тысячи блоков каждый тик.
 */
object FarmerFocus {
    const val NEAR_PLOTS = 24
    const val FAR_STEP = 12

    /**
     * null — места впритык, и без списка дропа не понять, влезет ли хоть один предмет.
     * Пустой слот принимает что угодно, полный инвентарь не принимает ничего.
     */
    fun harvestNow(bagFull: Boolean, emptySlot: Boolean): Boolean? {
        if (bagFull) return false
        if (emptySlot) return true
        return null
    }

    fun lookIndexes(
        count: Int,
        cursor: Int,
        distances: LongArray,
        near: Int = NEAR_PLOTS,
        far: Int = FAR_STEP,
    ): IntArray {
        if (count <= 0) return IntArray(0)
        if (count <= near) return IntArray(count) { it }
        val limit = minOf(near, count)
        val idx = IntArray(limit)
        val dist = LongArray(limit)
        var filled = 0
        for (i in 0 until count) {
            val d = distances[i]
            if (filled < limit) {
                var slot = filled
                while (slot > 0 && dist[slot - 1] > d) {
                    dist[slot] = dist[slot - 1]
                    idx[slot] = idx[slot - 1]
                    slot--
                }
                dist[slot] = d
                idx[slot] = i
                filled++
            } else if (d < dist[limit - 1]) {
                var slot = limit - 1
                while (slot > 0 && dist[slot - 1] > d) {
                    dist[slot] = dist[slot - 1]
                    idx[slot] = idx[slot - 1]
                    slot--
                }
                dist[slot] = d
                idx[slot] = i
            }
        }
        val chosen = LinkedHashSet<Int>()
        for (i in 0 until filled) chosen.add(idx[i])
        if (count <= near + far) {
            for (i in 0 until count) chosen.add(i)
            return chosen.toIntArray()
        }
        var index = Math.floorMod(cursor, count)
        var added = 0
        var guard = 0
        while (added < far && guard < count) {
            if (chosen.add(index)) added++
            index++
            if (index == count) index = 0
            guard++
        }
        return chosen.toIntArray()
    }
}
