package com.reallyusefulribbits.mod.loader.fabric

import com.reallyusefulribbits.mod.logic.ScanRadius
import com.reallyusefulribbits.mod.logic.ServerSettings
import com.reallyusefulribbits.mod.logic.SorcererAction
import com.reallyusefulribbits.mod.logic.SorcererTable
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Экран Mod Menu для `reallyusefulribbits-server.properties`: те же ключи и границы, что у файла. */
object RibbitsConfigScreen {
    private const val KEY = "reallyusefulribbits.configuration"

    fun create(parent: Screen): Screen {
        val current = FabricRuntime.settings
        var radius = current.scanRadius
        var chaos = current.chaos
        val points = current.points.toMutableMap()
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("$KEY.title"))
            .setSavingRunnable { FabricRuntime.saveSettings(ServerSettings(radius, chaos, points.toMap())) }
        val entries = builder.entryBuilder()

        val work = builder.getOrCreateCategory(Component.translatable("$KEY.work"))
        work.addEntry(entries.startTextDescription(Component.translatable("$KEY.fabric_note")).build())
        work.addEntry(
            entries.startIntSlider(Component.translatable("$KEY.work.scan_radius"), radius, ScanRadius.MIN, ScanRadius.MAX)
                .setDefaultValue(ScanRadius.DEFAULT)
                .setTooltip(Component.translatable("$KEY.work.scan_radius.tooltip"))
                .setSaveConsumer { radius = it }
                .build(),
        )

        val sorcerer = builder.getOrCreateCategory(Component.translatable("$KEY.sorcerer"))
        sorcerer.addEntry(
            entries.startIntSlider(Component.translatable("$KEY.sorcerer.chaos_level"), chaos, 0, 100)
                .setDefaultValue(SorcererTable.DEFAULT_CHAOS)
                .setTooltip(Component.translatable("$KEY.sorcerer.chaos_level.tooltip"))
                .setSaveConsumer { chaos = it }
                .build(),
        )

        val spells = builder.getOrCreateCategory(Component.translatable("$KEY.sorcerer.spells"))
        spells.addEntry(
            entries.startTextDescription(Component.translatable("$KEY.sorcerer.spells.tooltip", SorcererTable.DEFAULT_TOTAL)).build(),
        )
        for (action in SorcererAction.entries) {
            spells.addEntry(
                entries.startIntField(Component.translatable("$KEY.sorcerer.spells.${action.id}"), points[action] ?: action.defaultPoints)
                    .setDefaultValue(action.defaultPoints)
                    .setMin(0)
                    .setMax(SorcererTable.MAX_POINTS)
                    .setTooltip(
                        Component.translatable(
                            "$KEY.sorcerer.spells.default",
                            action.defaultPoints,
                            SorcererTable.percentText(action.defaultPoints),
                        ),
                    )
                    .setSaveConsumer { points[action] = it }
                    .build(),
            )
        }
        return builder.build()
    }
}
