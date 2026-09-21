package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerLevel

object RibbitBrain {
    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        RibbitSafety.ensure(ribbit)
        if (RibbitCombat.tickFlee(ribbit)) return
        if (ribbit.isPassenger) {
            data.riding = true
            ribbit.navigation.stop()
            ribbit.setFishing(false)
            ribbit.setWatering(false)
            return
        }
        if (data.riding) data.riding = false
        when (ribbit.professionKind()) {
            ProfessionKind.FISHERMAN -> FishermanAi.tick(level, ribbit)
            ProfessionKind.FARMER -> FarmerAi.tick(level, ribbit)
            ProfessionKind.MERCHANT -> MerchantAi.tick(level, ribbit)
            ProfessionKind.SORCERER -> SorcererAi.tick(level, ribbit)
            ProfessionKind.NITWIT -> NitwitAi.tick(level, ribbit)
            ProfessionKind.OTHER -> Unit
        }
    }
}
