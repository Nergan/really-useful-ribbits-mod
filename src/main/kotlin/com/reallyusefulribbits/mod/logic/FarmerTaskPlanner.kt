package com.reallyusefulribbits.mod.logic

enum class FarmerTask {
    DEPOSIT,
    HARVEST,
    TILL,
    PLANT,
    BONEMEAL,
    WATER,
    IDLE,
}

data class FarmerWorldView(
    val inventoryFull: Boolean,
    val inventoryHasItems: Boolean,
    val hasProduce: Boolean,
    val hasMatureCrop: Boolean,
    val hasTillable: Boolean,
    val hasEmptyFarmland: Boolean,
    val hasPlantable: Boolean,
    val hasImmatureCrop: Boolean,
    val hasBonemeal: Boolean = false,
    val hasBonemealTarget: Boolean = false,
)

object FarmerTaskPlanner {
    const val MIN_TASK_TICKS = 25
    const val SWITCH_COOLDOWN_TICKS = 15

    fun next(view: FarmerWorldView): FarmerTask {
        if (view.inventoryFull && view.inventoryHasItems) return FarmerTask.DEPOSIT
        if (view.hasMatureCrop) return FarmerTask.HARVEST
        if (view.hasProduce) return FarmerTask.DEPOSIT
        if (view.hasEmptyFarmland && view.hasPlantable) return FarmerTask.PLANT
        if (view.hasBonemeal && view.hasBonemealTarget) return FarmerTask.BONEMEAL
        if (view.inventoryHasItems) return FarmerTask.DEPOSIT
        if (view.hasImmatureCrop) return FarmerTask.WATER
        if (view.hasTillable) return FarmerTask.TILL
        return FarmerTask.IDLE
    }

    fun shouldKeep(
        current: FarmerTask,
        ticksOnTask: Int,
        view: FarmerWorldView,
    ): Boolean {
        if (current == FarmerTask.IDLE) return false
        val stillValid = when (current) {
            FarmerTask.DEPOSIT -> view.hasProduce || view.inventoryFull ||
                (view.inventoryHasItems && !view.hasEmptyFarmland && !view.hasMatureCrop && !view.hasBonemealTarget)
            FarmerTask.HARVEST -> view.hasMatureCrop
            FarmerTask.TILL -> view.hasTillable
            FarmerTask.PLANT -> view.hasEmptyFarmland && view.hasPlantable && !view.hasProduce
            FarmerTask.BONEMEAL -> view.hasBonemeal && view.hasBonemealTarget
            FarmerTask.WATER -> view.hasImmatureCrop
            FarmerTask.IDLE -> false
        }
        if (!stillValid) return false
        if (ticksOnTask < MIN_TASK_TICKS) return true
        val better = next(view)
        if (better == current) return true
        return ticksOnTask < MIN_TASK_TICKS + SWITCH_COOLDOWN_TICKS && priority(current) <= priority(better)
    }

    fun priority(task: FarmerTask): Int = when (task) {
        FarmerTask.DEPOSIT -> 0
        FarmerTask.HARVEST -> 1
        FarmerTask.PLANT -> 2
        FarmerTask.BONEMEAL -> 3
        FarmerTask.WATER -> 4
        FarmerTask.TILL -> 5
        FarmerTask.IDLE -> 6
    }
}
