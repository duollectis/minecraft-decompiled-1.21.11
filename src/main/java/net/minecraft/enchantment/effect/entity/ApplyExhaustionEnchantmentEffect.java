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
 * Эффект зачарования, добавляющий усталость игроку.
 * Применяется только к {@link PlayerEntity}; другие сущности игнорируются.
 */
public record ApplyExhaustionEnchantmentEffect(EnchantmentLevelBasedValue amount) implements EnchantmentEntityEffect {

	public static final MapCodec<ApplyExhaustionEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(EnchantmentLevelBasedValue.CODEC
							.fieldOf("amount")
							.forGetter(ApplyExhaustionEnchantmentEffect::amount))
					.apply(instance, ApplyExhaustionEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		if (user instanceof PlayerEntity player) {
			player.addExhaustion(amount.getValue(level));
		}
	}

	@Override
	public MapCodec<ApplyExhaustionEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
