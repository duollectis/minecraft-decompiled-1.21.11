package net.minecraft.enchantment.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

/**
 * Эффект зачарования, изменяющий свойства блока в заданной позиции (с опциональным смещением).
 * Применяет компонент {@link BlockStateComponent} к текущему состоянию блока.
 * Если состояние изменилось — обновляет блок и опционально генерирует игровое событие.
 */
public record SetBlockPropertiesEnchantmentEffect(
		BlockStateComponent properties,
		Vec3i offset,
		Optional<RegistryEntry<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {

	public static final MapCodec<SetBlockPropertiesEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BlockStateComponent.CODEC
							.fieldOf("properties")
							.forGetter(SetBlockPropertiesEnchantmentEffect::properties),
					Vec3i.CODEC
							.optionalFieldOf("offset", Vec3i.ZERO)
							.forGetter(SetBlockPropertiesEnchantmentEffect::offset),
					GameEvent.CODEC
							.optionalFieldOf("trigger_game_event")
							.forGetter(SetBlockPropertiesEnchantmentEffect::triggerGameEvent)
			).apply(instance, SetBlockPropertiesEnchantmentEffect::new)
	);

	public SetBlockPropertiesEnchantmentEffect(BlockStateComponent properties) {
		this(properties, Vec3i.ZERO, Optional.of(GameEvent.BLOCK_CHANGE));
	}

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		BlockPos targetPos = BlockPos.ofFloored(pos).add(offset);
		BlockState currentState = user.getEntityWorld().getBlockState(targetPos);
		BlockState newState = properties.applyToState(currentState);

		if (currentState == newState) {
			return;
		}

		if (user.getEntityWorld().setBlockState(targetPos, newState, 3)) {
			triggerGameEvent.ifPresent(event -> world.emitGameEvent(user, event, targetPos));
		}
	}

	@Override
	public MapCodec<SetBlockPropertiesEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
