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
 * {@code DamagedProperty}.
 */
public record DamagedProperty() implements BooleanProperty {

	public static final MapCodec<DamagedProperty> CODEC = MapCodec.unit(new DamagedProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		return stack.isDamaged();
	}

	@Override
	public MapCodec<DamagedProperty> getCodec() {
		return CODEC;
	}
}
