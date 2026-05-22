package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ClearAllEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;

/**
	 * Компонент защиты от смерти. Хранит список эффектов, применяемых к сущности
	 * в момент смерти (например, эффекты тотема бессмертия).
	 */
public record DeathProtectionComponent(List<ConsumeEffect> deathEffects) {

	public static final Codec<DeathProtectionComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(ConsumeEffect.CODEC
							.listOf()
							.optionalFieldOf("death_effects", List.of())
							.forGetter(DeathProtectionComponent::deathEffects))
					.apply(instance, DeathProtectionComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, DeathProtectionComponent> PACKET_CODEC = PacketCodec.tuple(
			ConsumeEffect.PACKET_CODEC.collect(PacketCodecs.toList()),
			DeathProtectionComponent::deathEffects,
			DeathProtectionComponent::new
	);
	public static final DeathProtectionComponent TOTEM_OF_UNDYING = new DeathProtectionComponent(
			List.of(
					new ClearAllEffectsConsumeEffect(),
					new ApplyEffectsConsumeEffect(
							List.of(
									new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1),
									new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1),
									new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0)
							)
					)
			)
	);

	/**
		 * Применяет все эффекты смерти к сущности. Вызывается в момент, когда предмет
		 * предотвращает гибель (например, тотем бессмертия поглощается из инвентаря).
		 *
		 * @param stack предмет, активировавший защиту
		 * @param entity сущность, которой применяются эффекты
		 */
	public void applyDeathEffects(ItemStack stack, LivingEntity entity) {
		for (ConsumeEffect consumeEffect : deathEffects) {
			consumeEffect.onConsume(entity.getEntityWorld(), stack, entity);
		}
	}
}
