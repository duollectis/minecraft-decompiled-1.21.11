package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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

@Environment(EnvType.CLIENT)
/**
 * {@code LookingAtBlockDebugHudEntry}.
 */
public class LookingAtBlockDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("looking_at_block");

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		Entity entity = MinecraftClient.getInstance().getCameraEntity();
		World world2 = (World) (SharedConstants.SHOW_SERVER_DEBUG_VALUES ? world : MinecraftClient.getInstance().world);
		if (entity != null && world2 != null) {
			HitResult hitResult = entity.raycast(20.0, 0.0F, false);
			List<String> list = new ArrayList<>();
			if (hitResult.getType() == HitResult.Type.BLOCK) {
				BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
				BlockState blockState = world2.getBlockState(blockPos);
				list.add(Formatting.UNDERLINE + "Targeted Block: " + blockPos.getX() + ", " + blockPos.getY() + ", "
						+ blockPos.getZ());
				list.add(String.valueOf(Registries.BLOCK.getId(blockState.getBlock())));

				for (Entry<Property<?>, Comparable<?>> entry : blockState.getEntries().entrySet()) {
					list.add(this.getBlockPropertyLine(entry));
				}

				blockState.streamTags().map(tag -> "#" + tag.id()).forEach(list::add);
			}

			lines.addLinesToSection(SECTION_ID, list);
		}
	}

	private String getBlockPropertyLine(Entry<Property<?>, Comparable<?>> propertyAndValue) {
		Property<?> property = propertyAndValue.getKey();
		Comparable<?> comparable = propertyAndValue.getValue();
		String string = Util.getValueAsString(property, comparable);
		if (Boolean.TRUE.equals(comparable)) {
			string = Formatting.GREEN + string;
		}
		else if (Boolean.FALSE.equals(comparable)) {
			string = Formatting.RED + string;
		}

		return property.getName() + ": " + string;
	}
}
