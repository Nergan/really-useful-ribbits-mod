package com.reallyusefulribbits.mod.attach

class PlayerVisualData(
    var upsideDown: Boolean = false,
    var flightTicks: Int = 0,
    var headRideDismountArmed: Boolean = false,
    var morphId: String = "",
    var morphFlight: Boolean = false,
    var spectatorTicks: Int = 0,
    var previousGameMode: String = "survival",
    var attributesSaved: Boolean = false,
    var savedMaxHealth: Double = 20.0,
    var savedScale: Double = 1.0,
    var savedSpeed: Double = 0.1,
    var savedAttack: Double = 1.0,
    var savedKnockback: Double = 0.0,
    var savedStep: Double = 0.6,
)
