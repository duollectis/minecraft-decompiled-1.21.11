package net.minecraft.world.explosion;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.Optional;

/**
 * Расширенное поведение взрыва с явной настройкой разрушения блоков,
 * урона сущностям, множителя отбрасывания и списка иммунных блоков.
 * <p>
 * Используется для взрывов с нестандартными правилами, например взрывов
 * из компонентов предметов или специальных игровых механик.
 */
public class AdvancedExplosionBehavior extends ExplosionBehavior {

	/**
	 * Сопротивление взрыву, которое делает блок фактически неразрушимым
	 * (аналогично бедроку).
	 */
	private static final float INDESTRUCTIBLE_BLAST_RESISTANCE = 3600000.0F;

	private final boolean destroyBlocks;
	private final boolean damageEntities;
	private final Optional<Float> knockbackModifier;
	private final Optional<RegistryEntryList<Block>> immuneBlocks;

	public AdvancedExplosionBehavior(
			boolean destroyBlocks,
			boolean damageEntities,
			Optional<Float> knockbackModifier,
			Optional<RegistryEntryList<Block>> immuneBlocks
	) {
		this.destroyBlocks = destroyBlocks;
		this.damageEntities = damageEntities;
		this.knockbackModifier = knockbackModifier;
		this.immuneBlocks = immuneBlocks;
	}

	/**
	 * Если задан список иммунных блоков — блоки из этого списка получают
	 * максимальное сопротивление взрыву (неразрушимы), остальные — нулевое.
	 * Если список не задан — делегирует базовой логике.
	 */
	@Override
	public Optional<Float> getBlastResistance(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState
	) {
		if (immuneBlocks.isEmpty()) {
			return super.getBlastResistance(explosion, world, pos, blockState, fluidState);
		}

		return blockState.isIn(immuneBlocks.get())
			? Optional.of(INDESTRUCTIBLE_BLAST_RESISTANCE)
			: Optional.empty();
	}

	@Override
	public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
		return destroyBlocks;
	}

	@Override
	public boolean shouldDamage(Explosion explosion, Entity entity) {
		return damageEntities;
	}

	/**
	 * Возвращает множитель отбрасывания. Летящие игроки не получают отбрасывания.
	 * Если множитель не задан явно — делегирует базовой логике.
	 */
	@Override
	public float getKnockbackModifier(Entity entity) {
		boolean isFlying = entity instanceof PlayerEntity player && player.getAbilities().flying;
		if (isFlying) {
			return 0.0F;
		}

		return knockbackModifier.orElseGet(() -> super.getKnockbackModifier(entity));
	}
}
