package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Запись отладочного HUD: жидкость, на которую смотрит игрок,
 * её идентификатор, состояния свойств и теги.
 */
@Environment(EnvType.CLIENT)
public class LookingAtFluidDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("looking_at_fluid");
	private static final double RAYCAST_DISTANCE = 20.0;

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();
		World targetWorld = (World) (SharedConstants.SHOW_SERVER_DEBUG_VALUES ? world : client.world);

		if (cameraEntity == null || targetWorld == null) {
			lines.addLinesToSection(SECTION_ID, List.of());
			return;
		}

		HitResult hitResult = cameraEntity.raycast(RAYCAST_DISTANCE, 0.0F, true);
		List<String> debugLines = new ArrayList<>();

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
			FluidState fluidState = targetWorld.getFluidState(blockPos);

			debugLines.add(Formatting.UNDERLINE + "Targeted Fluid: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
			debugLines.add(String.valueOf(Registries.FLUID.getId(fluidState.getFluid())));

			for (Entry<Property<?>, Comparable<?>> entry : fluidState.getEntries().entrySet()) {
				debugLines.add(getFluidPropertyLine(entry));
			}

			fluidState.streamTags().map(tag -> "#" + tag.id()).forEach(debugLines::add);
		}

		lines.addLinesToSection(SECTION_ID, debugLines);
	}

	private String getFluidPropertyLine(Entry<Property<?>, Comparable<?>> propertyAndValue) {
		Property<?> property = propertyAndValue.getKey();
		Comparable<?> value = propertyAndValue.getValue();
		String valueString = Util.getValueAsString(property, value);

		if (Boolean.TRUE.equals(value)) {
			valueString = Formatting.GREEN + valueString;
		} else if (Boolean.FALSE.equals(value)) {
			valueString = Formatting.RED + valueString;
		}

		return property.getName() + ": " + valueString;
	}
}
