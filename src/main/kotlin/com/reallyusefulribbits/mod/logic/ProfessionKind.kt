package com.reallyusefulribbits.mod.logic

enum class ProfessionKind {
    FISHERMAN,
    FARMER,
    MERCHANT,
    SORCERER,
    NITWIT,
    OTHER,
    ;

    companion object {
        fun fromId(path: String?): ProfessionKind = when (path?.lowercase()) {
            "fisherman" -> FISHERMAN
            "gardener", "farmer" -> FARMER
            "merchant" -> MERCHANT
            "sorcerer" -> SORCERER
            "nitwit" -> NITWIT
            else -> OTHER
        }
    }
}
