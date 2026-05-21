package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code FishingRodCastProperty}.
 */
public record FishingRodCastProperty() implements BooleanProperty {

	public static final MapCodec<FishingRodCastProperty> CODEC = MapCodec.unit(new FishingRodCastProperty());

	@Override
	public boolean test(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity entity,
			int seed,
			ItemDisplayContext displayContext
	) {
		if (entity instanceof PlayerEntity playerEntity && playerEntity.fishHook != null) {
			Arm arm = FishingBobberEntityRenderer.getArmHoldingRod(playerEntity);
			return entity.getStackInArm(arm) == stack;
		}
		else {
			return false;
		}
	}

	@Override
	public MapCodec<FishingRodCastProperty> getCodec() {
		return CODEC;
	}
}
