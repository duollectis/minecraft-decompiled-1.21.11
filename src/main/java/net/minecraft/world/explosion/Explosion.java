package net.minecraft.world.explosion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Контракт взрыва в игровом мире. Предоставляет доступ к параметрам взрыва:
 * позиции, мощности, источнику урона, типу разрушения и связанной сущности.
 * <p>
 * Реализуется через {@link ExplosionImpl} для серверной логики.
 */
public interface Explosion {

	/**
	 * Создаёт источник урона от взрыва, определяя атакующего и вызывающего.
	 *
	 * @param world  мир, в котором происходит взрыв
	 * @param source сущность-источник взрыва (может быть {@code null})
	 * @return источник урона от взрыва
	 */
	static DamageSource createDamageSource(World world, @Nullable Entity source) {
		return world.getDamageSources().explosion(source, getCausingEntity(source));
	}

	/**
	 * Определяет живую сущность, ответственную за взрыв (для начисления урона/статистики).
	 * <ul>
	 *   <li>ТНТ → владелец ТНТ</li>
	 *   <li>Живая сущность → она сама</li>
	 *   <li>Снаряд с живым владельцем → владелец снаряда</li>
	 *   <li>Иначе → {@code null}</li>
	 * </ul>
	 *
	 * @param entity сущность-источник взрыва
	 * @return живая сущность-виновник или {@code null}
	 */
	static @Nullable LivingEntity getCausingEntity(@Nullable Entity entity) {
		return switch (entity) {
			case TntEntity tntEntity -> tntEntity.getOwner();
			case LivingEntity livingEntity -> livingEntity;
			case ProjectileEntity projectile when projectile.getOwner() instanceof LivingEntity owner -> owner;
			case null, default -> null;
		};
	}

	ServerWorld getWorld();

	DestructionType getDestructionType();

	@Nullable LivingEntity getCausingEntity();

	@Nullable Entity getEntity();

	float getPower();

	Vec3d getPosition();

	boolean canTriggerBlocks();

	boolean preservesDecorativeEntities();

	/**
	 * Тип разрушения, определяющий поведение взрыва в отношении блоков.
	 */
	enum DestructionType {
		/** Блоки не разрушаются и не выпадают. */
		KEEP(false),
		/** Блоки разрушаются и выпадают как предметы. */
		DESTROY(true),
		/** Блоки разрушаются, но выпадение предметов случайное (с затуханием). */
		DESTROY_WITH_DECAY(true),
		/** Взрыв только активирует блоки (например, кнопки), не разрушая их. */
		TRIGGER_BLOCK(false);

		private final boolean destroysBlocks;

		DestructionType(boolean destroysBlocks) {
			this.destroysBlocks = destroysBlocks;
		}

		public boolean destroysBlocks() {
			return destroysBlocks;
		}
	}
}
