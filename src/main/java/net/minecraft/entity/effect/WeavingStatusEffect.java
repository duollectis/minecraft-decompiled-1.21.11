package net.minecraft.entity.effect;

import com.google.common.collect.Sets;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.rule.GameRules;

import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Эффект плетения (Weaving).
 *
 * <p>При гибели сущности размещает паутину в случайных позициях вокруг неё.
 * Паутина размещается только на заменяемых блоках с твёрдым основанием снизу.
 * Для игроков требуется только смерть; для мобов — также правило {@code doMobGriefing}.</p>
 */
class WeavingStatusEffect extends StatusEffect {

	/** Количество кандидатов для размещения паутины при поиске. */
	private static final int COBWEB_SEARCH_COUNT = 15;
	/** Радиус поиска позиций для паутины (блоки). */
	private static final int COBWEB_SEARCH_RADIUS = 1;
	/** Флаг обновления блока при установке паутины (notify + send to clients). */
	private static final int BLOCK_UPDATE_FLAGS = 3;
	/** Идентификатор мирового события для звука/частиц установки паутины. */
	private static final int COBWEB_PLACE_WORLD_EVENT = 3018;

	private final ToIntFunction<Random> cobwebCountFunction;

	protected WeavingStatusEffect(
			StatusEffectCategory category,
			int color,
			ToIntFunction<Random> cobwebCountFunction
	) {
		super(category, color, ParticleTypes.ITEM_COBWEB);
		this.cobwebCountFunction = cobwebCountFunction;
	}

	/**
	 * При гибели размещает паутину, если сущность — игрок или разрешено разрушение мобами.
	 */
	@Override
	public void onEntityRemoval(ServerWorld world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
		if (reason != Entity.RemovalReason.KILLED) {
			return;
		}

		boolean canPlace = entity instanceof PlayerEntity
				|| world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);

		if (canPlace) {
			tryPlaceCobweb(world, entity.getRandom(), entity.getBlockPos());
		}
	}

	/**
	 * Ищет подходящие позиции для паутины и размещает её.
	 *
	 * <p>Позиция подходит, если блок заменяем и под ним есть твёрдая поверхность сверху.
	 * Размещение прекращается, когда достигнуто целевое количество паутин.</p>
	 */
	private void tryPlaceCobweb(ServerWorld world, Random random, BlockPos origin) {
		Set<BlockPos> placed = Sets.newHashSet();
		int targetCount = cobwebCountFunction.applyAsInt(random);

		for (BlockPos candidate : BlockPos.iterateRandomly(random, COBWEB_SEARCH_COUNT, origin, COBWEB_SEARCH_RADIUS)) {
			BlockPos below = candidate.down();
			if (placed.contains(candidate)) {
				continue;
			}

			if (!world.getBlockState(candidate).isReplaceable()) {
				continue;
			}

			if (!world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP)) {
				continue;
			}

			placed.add(candidate.toImmutable());

			if (placed.size() >= targetCount) {
				break;
			}
		}

		for (BlockPos cobwebPos : placed) {
			world.setBlockState(cobwebPos, Blocks.COBWEB.getDefaultState(), BLOCK_UPDATE_FLAGS);
			world.syncWorldEvent(COBWEB_PLACE_WORLD_EVENT, cobwebPos, 0);
		}
	}
}
