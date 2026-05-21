package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BundleFullnessProperty}.
 */
public record BundleFullnessProperty() implements NumericProperty {

	public static final MapCodec<BundleFullnessProperty> CODEC = MapCodec.unit(new BundleFullnessProperty());

	@Override
	public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed) {
		return BundleItem.getAmountFilled(stack);
	}

	@Override
	public MapCodec<BundleFullnessProperty> getCodec() {
		return CODEC;
	}
}
