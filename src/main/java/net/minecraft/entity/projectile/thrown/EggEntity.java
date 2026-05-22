package net.minecraft.entity.projectile.thrown;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Яйцо — бросаемый снаряд, при столкновении с шансом 1/8 спавнящий цыплёнка,
 * а с шансом 1/32 — сразу четырёх цыплят.
 * <p>
 * При попадании в сущность наносит 0 урона (только триггер).
 * Статус-байт {@code 3} запускает клиентскую анимацию разбивания яйца.
 */
public class EggEntity extends ThrownItemEntity {

	private static final EntityDimensions EMPTY_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

	/** Статус-байт для клиента: воспроизвести анимацию разбивания. */
	private static final byte BREAK_STATUS = 3;

	/** Количество частиц при разбивании. */
	private static final int BREAK_PARTICLE_COUNT = 8;

	/** Разброс скорости частиц при разбивании. */
	private static final double BREAK_PARTICLE_SPREAD = 0.08;

	/** Вероятность спавна цыплёнка (1 из N). */
	private static final int CHICK_SPAWN_CHANCE = 8;

	/** Вероятность спавна четырёх цыплят вместо одного (1 из N). */
	private static final int QUAD_CHICK_CHANCE = 32;

	/** Количество цыплят при редком спавне. */
	private static final int QUAD_CHICK_COUNT = 4;

	/** Возраст цыплёнка при спавне (отрицательный = детёныш). */
	private static final int CHICK_BREEDING_AGE = -24000;

	public EggEntity(EntityType<? extends EggEntity> entityType, World world) {
		super(entityType, world);
	}

	public EggEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.EGG, owner, world, stack);
	}

	public EggEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.EGG, x, y, z, world, stack);
	}

	@Override
	public void handleStatus(byte status) {
		if (status != BREAK_STATUS) {
			return;
		}

		for (int index = 0; index < BREAK_PARTICLE_COUNT; index++) {
			getEntityWorld().addParticleClient(
					new ItemStackParticleEffect(ParticleTypes.ITEM, getStack()),
					getX(),
					getY(),
					getZ(),
					(random.nextFloat() - 0.5) * BREAK_PARTICLE_SPREAD,
					(random.nextFloat() - 0.5) * BREAK_PARTICLE_SPREAD,
					(random.nextFloat() - 0.5) * BREAK_PARTICLE_SPREAD
			);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		entityHitResult.getEntity().serverDamage(getDamageSources().thrown(this, getOwner()), 0.0F);
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (getEntityWorld().isClient()) {
			return;
		}

		if (random.nextInt(CHICK_SPAWN_CHANCE) == 0) {
			int chickCount = random.nextInt(QUAD_CHICK_CHANCE) == 0 ? QUAD_CHICK_COUNT : 1;

			for (int index = 0; index < chickCount; index++) {
				ChickenEntity chick = EntityType.CHICKEN.create(getEntityWorld(), SpawnReason.TRIGGERED);
				if (chick == null) {
					break;
				}

				chick.setBreedingAge(CHICK_BREEDING_AGE);
				chick.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
				Optional.ofNullable(getStack().get(DataComponentTypes.CHICKEN_VARIANT))
						.flatMap(variant -> variant.resolveEntry(getRegistryManager()))
						.ifPresent(chick::setVariant);

				if (!chick.recalculateDimensions(EMPTY_DIMENSIONS)) {
					break;
				}

				getEntityWorld().spawnEntity(chick);
			}
		}

		getEntityWorld().sendEntityStatus(this, BREAK_STATUS);
		discard();
	}

	@Override
	protected Item getDefaultItem() {
		return Items.EGG;
	}
}
