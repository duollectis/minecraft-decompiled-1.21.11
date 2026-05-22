package net.minecraft.entity.projectile.thrown;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * Плескательное зелье — при взрыве применяет эффекты ко всем сущностям
 * в радиусе {@link PotionEntity#POTION_EXPLOSION_RADIUS} блоков.
 * <p>
 * Сила эффекта масштабируется по расстоянию: чем дальше сущность от точки
 * взрыва, тем короче длительность эффекта. Мгновенные эффекты применяются
 * с масштабированной интенсивностью, длительные — с масштабированной длительностью.
 */
public class SplashPotionEntity extends PotionEntity {

	/** Квадрат радиуса взрыва для проверки попадания сущностей. */
	private static final double EXPLOSION_RADIUS_SQUARED = 16.0;

	/** Делитель для расчёта коэффициента силы эффекта по расстоянию. */
	private static final double EFFECT_RADIUS_DIVISOR = 4.0;

	/** Минимальная длительность эффекта (в тиках), ниже которой эффект не применяется. */
	private static final int MIN_EFFECT_DURATION = 20;

	/** Смещение при округлении длительности эффекта. */
	private static final float DURATION_ROUNDING_OFFSET = 0.5F;

	public SplashPotionEntity(EntityType<? extends SplashPotionEntity> entityType, World world) {
		super(entityType, world);
	}

	public SplashPotionEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.SPLASH_POTION, world, owner, stack);
	}

	public SplashPotionEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.SPLASH_POTION, world, x, y, z, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPLASH_POTION;
	}

	@Override
	public void spawnAreaEffectCloud(ServerWorld world, ItemStack stack, HitResult hitResult) {
		PotionContentsComponent contents = stack.getOrDefault(
				DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT
		);
		float durationScale = stack.getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F);
		Iterable<StatusEffectInstance> effects = contents.getEffects();

		Box hitBox = getBoundingBox().offset(hitResult.getPos().subtract(getEntityPos()));
		Box searchBox = hitBox.expand(POTION_EXPLOSION_RADIUS, 2.0, POTION_EXPLOSION_RADIUS);
		List<LivingEntity> nearbyEntities = getEntityWorld().getNonSpectatingEntities(LivingEntity.class, searchBox);

		if (nearbyEntities.isEmpty()) {
			return;
		}

		float toleranceMargin = ProjectileUtil.getToleranceMargin(this);
		Entity effectCause = getEffectCause();

		for (LivingEntity target : nearbyEntities) {
			if (!target.isAffectedBySplashPotions()) {
				continue;
			}

			double distanceSquared = hitBox.squaredMagnitude(target.getBoundingBox().expand(toleranceMargin));
			if (distanceSquared >= EXPLOSION_RADIUS_SQUARED) {
				continue;
			}

			double effectStrength = 1.0 - Math.sqrt(distanceSquared) / EFFECT_RADIUS_DIVISOR;

			for (StatusEffectInstance effectInstance : effects) {
				RegistryEntry<StatusEffect> effectType = effectInstance.getEffectType();

				if (effectType.value().isInstant()) {
					effectType.value().applyInstantEffect(
							world,
							this,
							getOwner(),
							target,
							effectInstance.getAmplifier(),
							effectStrength
					);
				} else {
					int scaledDuration = effectInstance.mapDuration(
							baseDuration -> (int) (effectStrength * baseDuration * durationScale + DURATION_ROUNDING_OFFSET)
					);
					StatusEffectInstance scaledEffect = new StatusEffectInstance(
							effectType,
							scaledDuration,
							effectInstance.getAmplifier(),
							effectInstance.isAmbient(),
							effectInstance.shouldShowParticles()
					);

					if (!scaledEffect.isDurationBelow(MIN_EFFECT_DURATION)) {
						target.addStatusEffect(scaledEffect, effectCause);
					}
				}
			}
		}
	}
}
