package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code MapRenderState}.
 */
public class MapRenderState implements FabricRenderState {

	public @Nullable Identifier texture;
	public final List<MapRenderState.Decoration> decorations = new ArrayList<>();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Decoration}.
	 */
	public static class Decoration implements FabricRenderState {

		public @Nullable Sprite sprite;
		public byte x;
		public byte z;
		public byte rotation;
		public boolean alwaysRendered;
		public @Nullable Text name;
	}
}
