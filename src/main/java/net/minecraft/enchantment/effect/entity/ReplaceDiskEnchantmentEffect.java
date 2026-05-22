package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.Optional;

/**
 * Эффект зачарования, заменяющий блоки в дисковой области вокруг заданной позиции.
 * Радиус и высота диска определяются уровнем зачарования. Блоки заменяются только
 * если они находятся в пределах круга (проверка по квадрату расстояния) и предикат выполнен.
 */
public record ReplaceDiskEnchantmentEffect(
		EnchantmentLevelBasedValue radius,
		EnchantmentLevelBasedValue height,
		Vec3i offset,
		Optional<BlockPredicate> predicate,
		BlockStateProvider blockState,
		Optional<RegistryEntry<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {

	public static final MapCodec<ReplaceDiskEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("radius")
							.forGetter(ReplaceDiskEnchantmentEffect::radius),
					EnchantmentLevelBasedValue.CODEC
							.fieldOf("height")
							.forGetter(ReplaceDiskEnchantmentEffect::height),
					Vec3i.CODEC
							.optionalFieldOf("offset", Vec3i.ZERO)
							.forGetter(ReplaceDiskEnchantmentEffect::offset),
					BlockPredicate.BASE_CODEC
							.optionalFieldOf("predicate")
							.forGetter(ReplaceDiskEnchantmentEffect::predicate),
					BlockStateProvider.TYPE_CODEC
							.fieldOf("block_state")
							.forGetter(ReplaceDiskEnchantmentEffect::blockState),
					GameEvent.CODEC
							.optionalFieldOf("trigger_game_event")
							.forGetter(ReplaceDiskEnchantmentEffect::triggerGameEvent)
			).apply(instance, ReplaceDiskEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		BlockPos center = BlockPos.ofFloored(pos).add(offset);
		Random random = user.getRandom();
		int diskRadius = (int) radius.getValue(level);
		int diskHeight = (int) height.getValue(level);

		for (BlockPos candidate : BlockPos.iterate(
				center.add(-diskRadius, 0, -diskRadius),
				center.add(diskRadius, Math.min(diskHeight - 1, 0), diskRadius)
		)) {
			boolean withinCircle = candidate.getSquaredDistanceFromCenter(pos.getX(), candidate.getY() + 0.5, pos.getZ())
					< MathHelper.square(diskRadius);
			boolean predicatePassed = predicate.map(p -> p.test(world, candidate)).orElse(true);

			if (withinCircle && predicatePassed && world.setBlockState(candidate, blockState.get(random, candidate))) {
				triggerGameEvent.ifPresent(event -> world.emitGameEvent(user, event, candidate));
			}
		}
	}

	@Override
	public MapCodec<ReplaceDiskEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
