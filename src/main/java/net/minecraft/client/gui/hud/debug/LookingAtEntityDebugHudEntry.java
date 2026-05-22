package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Запись отладочного HUD: сущность, на которую смотрит игрок,
 * и её тип из реестра.
 */
@Environment(EnvType.CLIENT)
public class LookingAtEntityDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("looking_at_entity");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		Entity targetedEntity = MinecraftClient.getInstance().targetedEntity;
		List<String> debugLines = new ArrayList<>();

		if (targetedEntity != null) {
			debugLines.add(Formatting.UNDERLINE + "Targeted Entity");
			debugLines.add(String.valueOf(Registries.ENTITY_TYPE.getId(targetedEntity.getType())));
		}

		lines.addLinesToSection(SECTION_ID, debugLines);
	}
}
