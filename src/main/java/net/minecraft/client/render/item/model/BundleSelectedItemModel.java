package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BundleSelectedItemModel}.
 */
public class BundleSelectedItemModel implements ItemModel {

	static final ItemModel INSTANCE = new BundleSelectedItemModel();

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
		ItemStack itemStack = BundleItem.getSelectedStack(stack);
		if (!itemStack.isEmpty()) {
			resolver.update(state, itemStack, displayContext, world, heldItemContext, seed);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public record Unbaked() implements ItemModel.Unbaked {

		public static final MapCodec<BundleSelectedItemModel.Unbaked>
				CODEC =
				MapCodec.unit(new BundleSelectedItemModel.Unbaked());

		@Override
		public MapCodec<BundleSelectedItemModel.Unbaked> getCodec() {
			return CODEC;
		}

		@Override
		public ItemModel bake(ItemModel.BakeContext context) {
			return BundleSelectedItemModel.INSTANCE;
		}

		@Override
		public void resolve(ResolvableModel.Resolver resolver) {
		}
	}
}
