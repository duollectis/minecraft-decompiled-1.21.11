package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Бутылка опыта — бросаемый снаряд, при столкновении спавнящий
 * случайное количество опыта (от 3 до 11 единиц).
 * <p>
 * Имеет повышенную гравитацию ({@link #getGravity()}) по сравнению с другими
 * бросаемыми предметами, что даёт характерную крутую траекторию.
 */
public class ExperienceBottleEntity extends ThrownItemEntity {

	/** Минимальное количество опыта при разбивании. */
	private static final int MIN_EXPERIENCE = 3;

	/** Случайный диапазон дополнительного опыта (два броска). */
	private static final int EXPERIENCE_RANDOM_RANGE = 5;

	/** Код мирового события для воспроизведения эффекта разбивания зелья опыта. */
	private static final int BREAK_WORLD_EVENT = 2002;

	/** Цвет частиц опыта (зелёный). */
	private static final int EXPERIENCE_PARTICLE_COLOR = -13083194;

	public ExperienceBottleEntity(EntityType<? extends ExperienceBottleEntity> entityType, World world) {
		super(entityType, world);
	}

	public ExperienceBottleEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.EXPERIENCE_BOTTLE, owner, world, stack);
	}

	public ExperienceBottleEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.EXPERIENCE_BOTTLE, x, y, z, world, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.EXPERIENCE_BOTTLE;
	}

	@Override
	protected double getGravity() {
		return 0.07;
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		serverWorld.syncWorldEvent(BREAK_WORLD_EVENT, getBlockPos(), EXPERIENCE_PARTICLE_COLOR);

		int experienceAmount = MIN_EXPERIENCE
				+ serverWorld.random.nextInt(EXPERIENCE_RANDOM_RANGE)
				+ serverWorld.random.nextInt(EXPERIENCE_RANDOM_RANGE);

		Vec3d spawnDirection = hitResult instanceof BlockHitResult blockHit
				? blockHit.getSide().getDoubleVector()
				: getVelocity().multiply(-1.0);

		ExperienceOrbEntity.spawn(serverWorld, hitResult.getPos(), spawnDirection, experienceAmount);
		discard();
	}
}
