package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.Optional;

/**
 * Эффект зачарования, заменяющий один блок в заданной позиции (с опциональным смещением).
 * Замена происходит только если предикат блока выполнен (или не задан).
 * После успешной замены опционально генерируется игровое событие.
 */
public record ReplaceBlockEnchantmentEffect(
		Vec3i offset,
		Optional<BlockPredicate> predicate,
		BlockStateProvider blockState,
		Optional<RegistryEntry<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {

	public static final MapCodec<ReplaceBlockEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Vec3i.CODEC
							.optionalFieldOf("offset", Vec3i.ZERO)
							.forGetter(ReplaceBlockEnchantmentEffect::offset),
					BlockPredicate.BASE_CODEC
							.optionalFieldOf("predicate")
							.forGetter(ReplaceBlockEnchantmentEffect::predicate),
					BlockStateProvider.TYPE_CODEC
							.fieldOf("block_state")
							.forGetter(ReplaceBlockEnchantmentEffect::blockState),
					GameEvent.CODEC
							.optionalFieldOf("trigger_game_event")
							.forGetter(ReplaceBlockEnchantmentEffect::triggerGameEvent)
			).apply(instance, ReplaceBlockEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		BlockPos targetPos = BlockPos.ofFloored(pos).add(offset);
		boolean predicatePassed = predicate.map(p -> p.test(world, targetPos)).orElse(true);

		if (predicatePassed && world.setBlockState(targetPos, blockState.get(user.getRandom(), targetPos))) {
			triggerGameEvent.ifPresent(event -> world.emitGameEvent(user, event, targetPos));
		}
	}

	@Override
	public MapCodec<ReplaceBlockEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
