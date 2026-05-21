package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code SpecialModelRenderer}.
 */
public interface SpecialModelRenderer<T> {

	void render(
			@Nullable T data,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int i
	);

	void collectVertices(Consumer<Vector3fc> consumer);

	@Nullable T getData(ItemStack stack);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BakeContext}.
	 */
	public interface BakeContext {

		LoadedEntityModels entityModelSet();

		SpriteHolder spriteHolder();

		PlayerSkinCache playerSkinRenderCache();

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Simple}.
		 */
		public record Simple(
				LoadedEntityModels entityModelSet,
				SpriteHolder spriteHolder,
				PlayerSkinCache playerSkinRenderCache
		)
				implements SpecialModelRenderer.BakeContext {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public interface Unbaked {

		@Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context);

		MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec();
	}
}
