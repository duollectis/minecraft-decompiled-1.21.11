package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BundleHasSelectedItemProperty}.
 */
public record BundleHasSelectedItemProperty() implements BooleanProperty {

	public static final MapCodec<BundleHasSelectedItemProperty>
			CODEC =
			MapCodec.unit(new BundleHasSelectedItemProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return BundleItem.hasSelectedStack(stack);
	}

	@Override
	public MapCodec<BundleHasSelectedItemProperty> getCodec() {
		return CODEC;
	}
}
