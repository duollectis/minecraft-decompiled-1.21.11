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
 * {@code BrokenProperty}.
 */
public record BrokenProperty() implements BooleanProperty {

	public static final MapCodec<BrokenProperty> CODEC = MapCodec.unit(new BrokenProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return stack.willBreakNextUse();
	}

	@Override
	public MapCodec<BrokenProperty> getCodec() {
		return CODEC;
	}
}
