package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
	 * Компонент эффектов подозрительного рагу. Хранит список эффектов статуса,
	 * применяемых к игроку при употреблении блюда.
	 */
public record SuspiciousStewEffectsComponent(List<SuspiciousStewEffectsComponent.StewEffect> effects) implements Consumable, TooltipAppender {

	public static final SuspiciousStewEffectsComponent DEFAULT = new SuspiciousStewEffectsComponent(List.of());
	public static final int DEFAULT_DURATION = 160;
	public static final Codec<SuspiciousStewEffectsComponent> CODEC = SuspiciousStewEffectsComponent.StewEffect.CODEC
			.listOf()
			.xmap(SuspiciousStewEffectsComponent::new, SuspiciousStewEffectsComponent::effects);
	public static final PacketCodec<RegistryByteBuf, SuspiciousStewEffectsComponent>
			PACKET_CODEC =
			SuspiciousStewEffectsComponent.StewEffect.PACKET_CODEC
					.collect(PacketCodecs.toList())
					.xmap(SuspiciousStewEffectsComponent::new, SuspiciousStewEffectsComponent::effects);

	/**
		 * Возвращает новый компонент с добавленным эффектом в конец списка.
		 *
		 * @param stewEffect добавляемый эффект рагу
		 * @return новый {@code SuspiciousStewEffectsComponent} с расширенным списком
		 */
	public SuspiciousStewEffectsComponent with(SuspiciousStewEffectsComponent.StewEffect stewEffect) {
		return new SuspiciousStewEffectsComponent(Util.withAppended(effects, stewEffect));
	}

	@Override
	public void onConsume(World world, LivingEntity user, ItemStack stack, ConsumableComponent consumable) {
		for (StewEffect stewEffect : effects) {
			user.addStatusEffect(stewEffect.createStatusEffectInstance());
		}
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		if (!type.isCreative()) {
			return;
		}

		List<StatusEffectInstance> instances = new ArrayList<>();

		for (StewEffect stewEffect : effects) {
			instances.add(stewEffect.createStatusEffectInstance());
		}

		PotionContentsComponent.buildTooltip(instances, textConsumer, 1.0F, context.getUpdateTickRate());
	}

	/**
		 * Отдельный эффект рагу: тип эффекта статуса и его длительность в тиках.
		 */
	public record StewEffect(RegistryEntry<StatusEffect> effect, int duration) {

		public static final Codec<SuspiciousStewEffectsComponent.StewEffect> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											StatusEffect.ENTRY_CODEC
													.fieldOf("id")
													.forGetter(SuspiciousStewEffectsComponent.StewEffect::effect),
											Codec.INT
													.lenientOptionalFieldOf("duration", DEFAULT_DURATION)
													.forGetter(SuspiciousStewEffectsComponent.StewEffect::duration)
									)
									.apply(instance, SuspiciousStewEffectsComponent.StewEffect::new)
		);
		public static final PacketCodec<RegistryByteBuf, SuspiciousStewEffectsComponent.StewEffect>
				PACKET_CODEC =
				PacketCodec.tuple(
						StatusEffect.ENTRY_PACKET_CODEC,
						SuspiciousStewEffectsComponent.StewEffect::effect,
						PacketCodecs.VAR_INT,
						SuspiciousStewEffectsComponent.StewEffect::duration,
						SuspiciousStewEffectsComponent.StewEffect::new
				);

		public StatusEffectInstance createStatusEffectInstance() {
			return new StatusEffectInstance(effect, duration);
		}
	}
}
