package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.Optional;

/**
 * Эффект зачарования, накладывающий случайный статус-эффект из заданного списка.
 * Длительность и усилитель выбираются случайно в диапазоне [{@code min}, {@code max}] для уровня зачарования.
 * Длительность конвертируется из секунд в тики (умножение на 20).
 */
public record ApplyMobEffectEnchantmentEffect(
		RegistryEntryList<StatusEffect> toApply,
		EnchantmentLevelBasedValue minDuration,
		EnchantmentLevelBasedValue maxDuration,
		EnchantmentLevelBasedValue minAmplifier,
		EnchantmentLevelBasedValue maxAmplifier
) implements EnchantmentEntityEffect {

	public static final MapCodec<ApplyMobEffectEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					RegistryCodecs
							.entryList(RegistryKeys.STATUS_EFFECT)
							.fieldOf("to_apply")
							.forGetter(ApplyMobEffectEnchantmentEffect::toApply),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("min_duration")
							.forGetter(ApplyMobEffectEnchantmentEffect::minDuration),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("max_duration")
							.forGetter(ApplyMobEffectEnchantmentEffect::maxDuration),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("min_amplifier")
							.forGetter(ApplyMobEffectEnchantmentEffect::minAmplifier),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("max_amplifier")
							.forGetter(ApplyMobEffectEnchantmentEffect::maxAmplifier)
			).apply(instance, ApplyMobEffectEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		if (!(user instanceof LivingEntity livingEntity)) {
			return;
		}

		Random random = livingEntity.getRandom();
		Optional<RegistryEntry<StatusEffect>> chosenEffect = toApply.getRandom(random);

		if (chosenEffect.isEmpty()) {
			return;
		}

		int durationTicks = Math.round(
				MathHelper.nextBetween(random, minDuration.getValue(level), maxDuration.getValue(level)) * 20.0F
		);
		int amplifier = Math.max(
				0,
				Math.round(MathHelper.nextBetween(random, minAmplifier.getValue(level), maxAmplifier.getValue(level)))
		);

		livingEntity.addStatusEffect(new StatusEffectInstance(chosenEffect.get(), durationTicks, amplifier));
	}

	@Override
	public MapCodec<ApplyMobEffectEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
