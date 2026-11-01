package com.reallyusefulribbits.mod.event

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.logic.SorcererAction
import com.reallyusefulribbits.mod.profession.SorcererAi
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/** `/reallyusefulribbits cast <ключ> [игрок]`: оператор проверяет заклинание у ближайшего риббита. */
object SorcererCommand {
    private const val REACH = 32.0

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("reallyusefulribbits")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("cast").then(
                        Commands.argument("spell", StringArgumentType.word())
                            .suggests { _, builder -> SharedSuggestionProvider.suggest(SorcererAction.entries.map { it.id }, builder) }
                            .executes { cast(it, it.source.playerOrException) }
                            .then(
                                Commands.argument("target", EntityArgument.player())
                                    .executes { cast(it, EntityArgument.getPlayer(it, "target")) },
                            ),
                    ),
                ),
        )
    }

    private fun cast(context: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        val id = StringArgumentType.getString(context, "spell")
        val action = SorcererAction.byId(id)
        if (action == null) {
            context.source.sendFailure(Component.literal("Unknown spell $id"))
            return 0
        }
        val level = player.serverLevel()
        val ribbit = level.getEntitiesOfClass(RibbitEntity::class.java, player.boundingBox.inflate(REACH)) { it.isAlive }
            .minByOrNull { it.distanceToSqr(player) }
        if (ribbit == null) {
            context.source.sendFailure(Component.literal("No ribbit within ${REACH.toInt()} blocks of ${player.scoreboardName}"))
            return 0
        }
        val applied = SorcererAi.forceCast(level, ribbit, player, action)
        ReallyUsefulRibbitsMod.LOGGER.info("Operator cast {} on {}: {}", id, player.scoreboardName, if (applied) "applied" else "could not happen")
        if (!applied) {
            context.source.sendFailure(Component.literal("$id could not happen here"))
            return 0
        }
        context.source.sendSuccess({ Component.literal("Cast $id on ${player.scoreboardName}") }, true)
        return 1
    }
}
