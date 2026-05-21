package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CatEntityRenderState}.
 */
public class CatEntityRenderState extends FelineEntityRenderState {

	private static final Identifier DEFAULT_TEXTURE = Identifier.ofVanilla("textures/entity/cat/tabby.png");
	public Identifier texture = DEFAULT_TEXTURE;
	public boolean nearSleepingPlayer;
	public @Nullable DyeColor collarColor;
}
