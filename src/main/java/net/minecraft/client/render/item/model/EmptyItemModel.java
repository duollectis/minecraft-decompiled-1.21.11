package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code EmptyItemModel}.
 */
public class EmptyItemModel implements ItemModel {

	public static final ItemModel INSTANCE = new EmptyItemModel();

	@Override
	public void update(
			ItemRenderState state,
			ItemStack stack,
			ItemModelManager resolver,
			ItemDisplayContext displayContext,
			@Nullable ClientWorld world,
			@Nullable HeldItemContext heldItemContext,
			int seed
	) {
		state.addModelKey(this);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked() implements ItemModel.Unbaked {

		public static final MapCodec<EmptyItemModel.Unbaked> CODEC = MapCodec.unit(EmptyItemModel.Unbaked::new);

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			return EmptyItemModel.INSTANCE;
		}

		@Override
		public MapCodec<EmptyItemModel.Unbaked> getCodec() {
			return CODEC;
		}
	}
}
