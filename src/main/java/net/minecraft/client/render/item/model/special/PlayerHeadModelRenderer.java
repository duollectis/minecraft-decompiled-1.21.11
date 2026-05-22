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

/**
 * Рендерер головы игрока как предмета инвентаря.
 * Загружает скин через {@link PlayerSkinCache} по компоненту {@code PROFILE} стека предмета.
 */
@Environment(EnvType.CLIENT)
public class PlayerHeadModelRenderer implements SpecialModelRenderer<PlayerSkinCache.Entry> {

	private final PlayerSkinCache playerSkinCache;
	private final SkullBlockEntityModel model;

	PlayerHeadModelRenderer(PlayerSkinCache playerSkinCache, SkullBlockEntityModel model) {
		this.playerSkinCache = playerSkinCache;
		this.model = model;
	}

	public void render(
			PlayerSkinCache.@Nullable Entry entry,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	) {
		RenderLayer layer = entry != null ? entry.getRenderLayer() : PlayerSkinCache.DEFAULT_RENDER_LAYER;

		SkullBlockEntityRenderer.render(
				null,
				180.0F,
				0.0F,
				matrices,
				queue,
				light,
				model,
				layer,
				seed,
				null
		);
	}

	@Override
	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrices = new MatrixStack();
		matrices.translate(0.5F, 0.0F, 0.5F);
		matrices.scale(-1.0F, -1.0F, 1.0F);
		model.getRootPart().collectVertices(matrices, consumer);
	}

	public PlayerSkinCache.@Nullable Entry getData(ItemStack itemStack) {
		ProfileComponent profile = itemStack.get(DataComponentTypes.PROFILE);
		return profile == null ? null : playerSkinCache.get(profile);
	}

	/**
	 * Несериализованный дескриптор рендерера головы игрока.
	 * Не содержит параметров — всегда использует тип {@link SkullBlock.Type#PLAYER}.
	 */
	@Environment(EnvType.CLIENT)
	public record Unbaked() implements SpecialModelRenderer.Unbaked {

		public static final MapCodec<PlayerHeadModelRenderer.Unbaked> CODEC = MapCodec.unit(
				PlayerHeadModelRenderer.Unbaked::new
		);

		@Override
		public MapCodec<PlayerHeadModelRenderer.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) {
			SkullBlockEntityModel skullModel = SkullBlockEntityRenderer.getModels(
					context.entityModelSet(),
					SkullBlock.Type.PLAYER
			);

			return skullModel == null ? null : new PlayerHeadModelRenderer(
					context.playerSkinRenderCache(),
					skullModel
			);
		}
	}
}
