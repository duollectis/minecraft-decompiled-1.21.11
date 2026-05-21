package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerHeadModelRenderer}.
 */
public class PlayerHeadModelRenderer implements SpecialModelRenderer<PlayerSkinCache.Entry> {

	private final PlayerSkinCache playerSkinCache;
	private final SkullBlockEntityModel model;

	PlayerHeadModelRenderer(PlayerSkinCache playerSkinCache, SkullBlockEntityModel model) {
		this.playerSkinCache = playerSkinCache;
		this.model = model;
	}

	public void render(
			PlayerSkinCache.@Nullable Entry entry,
			ItemDisplayContext itemDisplayContext,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int i,
			int j,
			boolean bl,
			int k
	) {
		RenderLayer renderLayer = entry != null ? entry.getRenderLayer() : PlayerSkinCache.DEFAULT_RENDER_LAYER;
		SkullBlockEntityRenderer.render(
				null,
				180.0F,
				0.0F,
				matrixStack,
				orderedRenderCommandQueue,
				i,
				this.model,
				renderLayer,
				k,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.translate(0.5F, 0.0F, 0.5F);
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		this.model.getRootPart().collectVertices(matrixStack, consumer);
	}

	public PlayerSkinCache.@Nullable Entry getData(ItemStack itemStack) {
		ProfileComponent profileComponent = itemStack.get(DataComponentTypes.PROFILE);
		return profileComponent == null ? null : this.playerSkinCache.get(profileComponent);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<PlayerHeadModelRenderer.Unbaked>
				CODEC =
				MapCodec.unit(PlayerHeadModelRenderer.Unbaked::new);

		@Override
		public MapCodec<PlayerHeadModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			SkullBlockEntityModel
					skullBlockEntityModel =
					SkullBlockEntityRenderer.getModels(context.entityModelSet(), SkullBlock.Type.PLAYER);
			return skullBlockEntityModel == null ? null : new PlayerHeadModelRenderer(
					context.playerSkinRenderCache(),
					skullBlockEntityModel
			);
		}
	}
}
