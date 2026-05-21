package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * {@code ApplyImpulseEnchantmentEffect}.
 */
public record ApplyImpulseEnchantmentEffect(
		Vec3d direction,
		Vec3d coordinateScale,
		EnchantmentLevelBasedValue magnitude
) implements EnchantmentEntityEffect {

	public static final MapCodec<ApplyImpulseEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Vec3d.CODEC.fieldOf("direction").forGetter(ApplyImpulseEnchantmentEffect::direction),
					                    Vec3d.CODEC.fieldOf("coordinate_scale").forGetter(ApplyImpulseEnchantmentEffect::coordinateScale),
					                    EnchantmentLevelBasedValue.CODEC
							                    .fieldOf("magnitude")
							                    .forGetter(ApplyImpulseEnchantmentEffect::magnitude)
			                    )
			                    .apply(instance, ApplyImpulseEnchantmentEffect::new)
	);
	private static final int CURRENT_EXPLOSION_RESET_GRACE_TIME = 10;

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		Vec3d vec3d = user.getRotationVector();
		Vec3d
				vec3d2 =
				vec3d
						.transformLocalPos(this.direction)
						.multiply(this.coordinateScale)
						.multiply(this.magnitude.getValue(level));
		user.addVelocityInternal(vec3d2);
		user.knockedBack = true;
		user.velocityDirty = true;
		if (user instanceof PlayerEntity playerEntity) {
			playerEntity.setCurrentExplosionResetGraceTime(10);
		}
	}

	@Override
	public MapCodec<ApplyImpulseEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
