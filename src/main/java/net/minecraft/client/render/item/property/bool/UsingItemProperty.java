package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code UsingItemProperty}.
 */
public record UsingItemProperty() implements BooleanProperty {

	public static final MapCodec<UsingItemProperty> CODEC = MapCodec.unit(new UsingItemProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return entity == null ? false : entity.isUsingItem() && entity.getActiveItem() == stack;
	}

	@Override
	public MapCodec<UsingItemProperty> getCodec() {
		return CODEC;
	}
}
