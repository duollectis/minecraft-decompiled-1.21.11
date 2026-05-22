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
 * Эффект зачарования, применяющий импульс (толчок) к сущности в направлении её взгляда.
 * Направление трансформируется в локальное пространство сущности, масштабируется
 * по {@code coordinateScale} и умножается на {@code magnitude} от уровня зачарования.
 */
public record ApplyImpulseEnchantmentEffect(
		Vec3d direction,
		Vec3d coordinateScale,
		EnchantmentLevelBasedValue magnitude
) implements EnchantmentEntityEffect {

	private static final int EXPLOSION_RESET_GRACE_TICKS = 10;

	public static final MapCodec<ApplyImpulseEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Vec3d.CODEC.fieldOf("direction").forGetter(ApplyImpulseEnchantmentEffect::direction),
					Vec3d.CODEC.fieldOf("coordinate_scale").forGetter(ApplyImpulseEnchantmentEffect::coordinateScale),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("magnitude")
							.forGetter(ApplyImpulseEnchantmentEffect::magnitude)
			).apply(instance, ApplyImpulseEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		Vec3d rotationVector = user.getRotationVector();
		Vec3d impulse = rotationVector
				.transformLocalPos(direction)
				.multiply(coordinateScale)
				.multiply(magnitude.getValue(level));

		user.addVelocityInternal(impulse);
		user.knockedBack = true;
		user.velocityDirty = true;

		if (user instanceof PlayerEntity player) {
			player.setCurrentExplosionResetGraceTime(EXPLOSION_RESET_GRACE_TICKS);
		}
	}

	@Override
	public MapCodec<ApplyImpulseEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
