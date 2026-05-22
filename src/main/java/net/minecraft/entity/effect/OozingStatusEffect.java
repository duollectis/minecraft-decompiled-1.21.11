package net.minecraft.entity.effect;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Эффект слизи (Oozing).
 *
 * <p>При гибели сущности спавнит слаймов размера {@value #SPAWNED_SLIME_SIZE} рядом с ней.
 * Количество спавнящихся слаймов ограничено правилом {@link GameRules#MAX_ENTITY_CRAMMING}.</p>
 */
class OozingStatusEffect extends StatusEffect {

	/** Радиус поиска существующих слаймов для проверки лимита скученности (блоки). */
	private static final int SLIME_SEARCH_RADIUS = 2;
	/** Вертикальное смещение точки спавна слайма от позиции сущности. */
	private static final double SLIME_SPAWN_Y_OFFSET = 0.5;

	public static final int SPAWNED_SLIME_SIZE = 2;

	private final ToIntFunction<Random> slimeCountFunction;

	protected OozingStatusEffect(StatusEffectCategory category, int color, ToIntFunction<Random> slimeCountFunction) {
		super(category, color, ParticleTypes.ITEM_SLIME);
		this.slimeCountFunction = slimeCountFunction;
	}

	/**
	 * Вычисляет фактическое количество слаймов для спавна с учётом лимита скученности.
	 *
	 * @param maxEntityCramming лимит скученности (0 = без ограничений)
	 * @param slimeCounter      счётчик существующих слаймов в радиусе
	 * @param potentialSlimes   желаемое количество слаймов
	 * @return фактическое количество слаймов для спавна
	 */
	@VisibleForTesting
	protected static int getSlimesToSpawn(
			int maxEntityCramming,
			SlimeCounter slimeCounter,
			int potentialSlimes
	) {
		return maxEntityCramming < 1
				? potentialSlimes
				: MathHelper.clamp(0, maxEntityCramming - slimeCounter.count(maxEntityCramming), potentialSlimes);
	}

	/**
	 * При гибели сущности спавнит слаймов с учётом лимита скученности мира.
	 */
	@Override
	public void onEntityRemoval(ServerWorld world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.KILLED) {
			return;
		}

		int slimeCount = slimeCountFunction.applyAsInt(entity.getRandom());
		int maxCramming = world.getGameRules().getValue(GameRules.MAX_ENTITY_CRAMMING);
		int toSpawn = getSlimesToSpawn(maxCramming, SlimeCounter.around(entity), slimeCount);

		for (int index = 0; index < toSpawn; index++) {
			spawnSlime(entity.getEntityWorld(), entity.getX(), entity.getY() + SLIME_SPAWN_Y_OFFSET, entity.getZ());
		}
	}

	private void spawnSlime(World world, double x, double y, double z) {
		SlimeEntity slime = EntityType.SLIME.create(world, SpawnReason.TRIGGERED);
		if (slime == null) {
			return;
		}

		slime.setSize(SPAWNED_SLIME_SIZE, true);
		slime.refreshPositionAndAngles(x, y, z, world.getRandom().nextFloat() * 360.0F, 0.0F);
		world.spawnEntity(slime);
	}

	// -------------------------------------------------------------------------
	// Вложенные типы
	// -------------------------------------------------------------------------

	/**
	 * Функциональный интерфейс для подсчёта слаймов в радиусе от сущности.
	 * Позволяет подменять реализацию в тестах.
	 */
	@FunctionalInterface
	protected interface SlimeCounter {

		/**
		 * Считает слаймов в радиусе, не превышая {@code limit}.
		 *
		 * @param limit максимальное количество для подсчёта
		 * @return количество найденных слаймов (не более {@code limit})
		 */
		int count(int limit);

		/**
		 * Создаёт счётчик, ищущий слаймов в радиусе {@value SLIME_SEARCH_RADIUS} блоков от сущности.
		 */
		static SlimeCounter around(LivingEntity entity) {
			return limit -> {
				List<SlimeEntity> nearby = new ArrayList<>();
				entity.getEntityWorld().collectEntitiesByType(
						EntityType.SLIME,
						entity.getBoundingBox().expand(SLIME_SEARCH_RADIUS),
						slime -> slime != entity,
						nearby,
						limit
				);
				return nearby.size();
			};
		}
	}
}
