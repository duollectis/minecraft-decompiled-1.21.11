package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import net.minecraft.world.rule.ServerGameRules;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code InitialWorldOptions}.
 */
public record InitialWorldOptions(
		WorldCreator.Mode selectedGameMode,
		ServerGameRules gameRuleOverwrites,
		@Nullable RegistryKey<FlatLevelGeneratorPreset> flatLevelPreset
) {
}
