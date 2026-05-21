package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code UseDurationProperty}.
 */
public record UseDurationProperty(boolean remaining) implements NumericProperty {

	public static final MapCodec<UseDurationProperty> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.BOOL.optionalFieldOf("remaining", false).forGetter(UseDurationProperty::remaining))
					.apply(instance, UseDurationProperty::new)
	);

	@Override
	public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable HeldItemContext context, int seed) {
		LivingEntity livingEntity = context == null ? null : context.getEntity();
		if (livingEntity != null && livingEntity.getActiveItem() == stack) {
			return this.remaining ? livingEntity.getItemUseTimeLeft() : getTicksUsedSoFar(stack, livingEntity);
		}
		else {
			return 0.0F;
		}
	}

	@Override
	public MapCodec<UseDurationProperty> getCodec() {
		return CODEC;
	}

	public static int getTicksUsedSoFar(ItemStack stack, LivingEntity user) {
		return stack.getMaxUseTime(user) - user.getItemUseTimeLeft();
	}
}
