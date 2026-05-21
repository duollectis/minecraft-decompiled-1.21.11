package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code CarriedProperty}.
 */
public record CarriedProperty() implements BooleanProperty {

	public static final MapCodec<CarriedProperty> CODEC = MapCodec.unit(new CarriedProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return entity instanceof ClientPlayerEntity clientPlayerEntity
				&& clientPlayerEntity.currentScreenHandler.getCursorStack() == stack;
	}

	@Override
	public MapCodec<CarriedProperty> getCodec() {
		return CODEC;
	}
}
