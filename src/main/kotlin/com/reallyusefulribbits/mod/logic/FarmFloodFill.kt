package com.reallyusefulribbits.mod.logic

object FarmFloodFill {
    fun fill(
        origin: GridPos,
        radius: Int,
        maxBlocks: Int = 2048,
        isFarm: (GridPos) -> Boolean,
    ): List<GridPos> {
        val r = ScanRadius.clamp(radius)
        val maxDist = r * r
        if (!isFarm(origin) || origin.distSq(origin) > maxDist) return emptyList()
        val seen = HashSet<GridPos>()
        val queue = ArrayDeque<GridPos>()
        val out = ArrayList<GridPos>()
        seen.add(origin)
        queue.add(origin)
        while (queue.isNotEmpty() && out.size < maxBlocks) {
            val pos = queue.removeFirst()
            if (!isFarm(pos) || pos.distSq(origin) > maxDist) continue
            out.add(pos)
            for (next in pos.neighborsFarm()) {
                if (seen.add(next) && next.distSq(origin) <= maxDist) {
                    queue.add(next)
                }
            }
        }
        return out
    }
}
