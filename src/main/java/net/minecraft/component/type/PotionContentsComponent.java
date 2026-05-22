package net.minecraft.component.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
	 * Компонент содержимого зелья. Хранит базовое зелье, кастомный цвет, дополнительные эффекты
	 * и кастомное имя. Управляет смешиванием цветов и применением эффектов.
	 */
public record PotionContentsComponent(
		Optional<RegistryEntry<Potion>> potion,
		Optional<Integer> customColor,
		List<StatusEffectInstance> customEffects,
		Optional<String> customName
) implements Consumable, TooltipAppender {

	public static final PotionContentsComponent
			DEFAULT =
			new PotionContentsComponent(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
	private static final Text NONE_TEXT = Text.translatable("effect.none").formatted(Formatting.GRAY);
	/** Серо-синий цвет зелья без эффектов (ARGB #FF385DC6). */
	public static final int EFFECTLESS_COLOR = (int) 0xFF385DC6L;
	private static final Codec<PotionContentsComponent> BASE_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContentsComponent::potion),
										Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContentsComponent::customColor),
										StatusEffectInstance.CODEC
												.listOf()
												.optionalFieldOf("custom_effects", List.of())
												.forGetter(PotionContentsComponent::customEffects),
										Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContentsComponent::customName)
								)
								.apply(instance, PotionContentsComponent::new)
	);
	public static final Codec<PotionContentsComponent>
			CODEC =
			Codec.withAlternative(BASE_CODEC, Potion.CODEC, PotionContentsComponent::new);
	public static final PacketCodec<RegistryByteBuf, PotionContentsComponent> PACKET_CODEC = PacketCodec.tuple(
			Potion.PACKET_CODEC.collect(PacketCodecs::optional),
			PotionContentsComponent::potion,
			PacketCodecs.INTEGER.collect(PacketCodecs::optional),
			PotionContentsComponent::customColor,
			StatusEffectInstance.PACKET_CODEC.collect(PacketCodecs.toList()),
			PotionContentsComponent::customEffects,
			PacketCodecs.STRING.collect(PacketCodecs::optional),
			PotionContentsComponent::customName,
			PotionContentsComponent::new
	);

	public PotionContentsComponent(RegistryEntry<Potion> potion) {
		this(Optional.of(potion), Optional.empty(), List.of(), Optional.empty());
	}

	/**
		 * Создаёт стек предмета с установленным компонентом зелья.
		 *
		 * @param item   тип предмета (например, зелье, стрела)
		 * @param potion запись реестра зелья
		 * @return новый {@link ItemStack} с компонентом {@code POTION_CONTENTS}
		 */
	public static ItemStack createStack(Item item, RegistryEntry<Potion> potion) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));
		return stack;
	}

	public boolean matches(RegistryEntry<Potion> potionEntry) {
		return potion.isPresent() && potion.get().matches(potionEntry) && customEffects.isEmpty();
	}

	public Iterable<StatusEffectInstance> getEffects() {
		if (potion.isEmpty()) {
			return customEffects;
		}

		return customEffects.isEmpty()
				? potion.get().value().getEffects()
				: Iterables.concat(potion.get().value().getEffects(), customEffects);
	}

	public void forEachEffect(Consumer<StatusEffectInstance> effectConsumer, float durationMultiplier) {
		if (potion.isPresent()) {
			for (StatusEffectInstance effect : potion.get().value().getEffects()) {
				effectConsumer.accept(effect.withScaledDuration(durationMultiplier));
			}
		}

		for (StatusEffectInstance effect : customEffects) {
			effectConsumer.accept(effect.withScaledDuration(durationMultiplier));
		}
	}

	public PotionContentsComponent with(RegistryEntry<Potion> newPotion) {
		return new PotionContentsComponent(Optional.of(newPotion), customColor, customEffects, customName);
	}

	public PotionContentsComponent with(StatusEffectInstance customEffect) {
		return new PotionContentsComponent(
				potion,
				customColor,
				Util.withAppended(customEffects, customEffect),
				customName
		);
	}

	public int getColor() {
		return getColor(EFFECTLESS_COLOR);
	}

	public int getColor(int defaultColor) {
		return customColor.isPresent()
				? customColor.get()
				: mixColors(getEffects()).orElse(defaultColor);
	}

	public Text getName(String prefix) {
		String name = customName
				.or(() -> potion.map(entry -> entry.value().getBaseName()))
				.orElse("empty");
		return Text.translatable(prefix + name);
	}

	/**
		 * Смешивает цвета всех видимых эффектов с учётом их усилителей (amplifier + 1 как вес).
		 * Возвращает пустой OptionalInt если ни один эффект не показывает частицы.
		 *
		 * @param effects итерируемый набор эффектов
		 * @return смешанный ARGB-цвет или пустой OptionalInt
		 */
	public static OptionalInt mixColors(Iterable<StatusEffectInstance> effects) {
		int totalRed = 0;
		int totalGreen = 0;
		int totalBlue = 0;
		int totalWeight = 0;

		for (StatusEffectInstance effect : effects) {
			if (!effect.shouldShowParticles()) {
				continue;
			}

			int color = effect.getEffectType().value().getColor();
			int weight = effect.getAmplifier() + 1;
			totalRed += weight * ColorHelper.getRed(color);
			totalGreen += weight * ColorHelper.getGreen(color);
			totalBlue += weight * ColorHelper.getBlue(color);
			totalWeight += weight;
		}

		if (totalWeight == 0) {
			return OptionalInt.empty();
		}

		return OptionalInt.of(ColorHelper.getArgb(
				totalRed / totalWeight,
				totalGreen / totalWeight,
				totalBlue / totalWeight
		));
	}

	public boolean hasEffects() {
		return !customEffects.isEmpty()
				|| (potion.isPresent() && !potion.get().value().getEffects().isEmpty());
	}

	public List<StatusEffectInstance> customEffects() {
		return Lists.transform(customEffects, StatusEffectInstance::new);
	}

	/**
		 * Применяет все эффекты зелья к сущности с учётом множителя длительности.
		 * Мгновенные эффекты применяются через {@code applyInstantEffect}, остальные — через {@code addStatusEffect}.
		 *
		 * @param user               сущность-получатель
		 * @param durationMultiplier множитель длительности эффектов
		 */
	public void apply(LivingEntity user, float durationMultiplier) {
		if (!(user.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		PlayerEntity player = user instanceof PlayerEntity playerEntity ? playerEntity : null;
		forEachEffect(
				effect -> {
					if (effect.getEffectType().value().isInstant()) {
						effect.getEffectType().value().applyInstantEffect(
								serverWorld, player, player, user, effect.getAmplifier(), 1.0
						);
					}
					else {
						user.addStatusEffect(effect);
					}
				},
				durationMultiplier
		);
	}

	/**
		 * Строит тултип для набора эффектов зелья, включая атрибутные модификаторы.
		 * Эффекты с длительностью выше 20 тиков отображаются с временем действия.
		 *
		 * @param effects            набор эффектов для отображения
		 * @param textConsumer       получатель строк тултипа
		 * @param durationMultiplier множитель длительности для отображения
		 * @param tickRate           частота тиков сервера
		 */
	public static void buildTooltip(
			Iterable<StatusEffectInstance> effects,
			Consumer<Text> textConsumer,
			float durationMultiplier,
			float tickRate
	) {
		List<Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier>> attributeModifiers = Lists.newArrayList();
		boolean hasNoEffects = true;

		for (StatusEffectInstance effectInstance : effects) {
			hasNoEffects = false;
			RegistryEntry<StatusEffect> effectType = effectInstance.getEffectType();
			int amplifier = effectInstance.getAmplifier();
			effectType.value().forEachAttributeModifier(
					amplifier, (attribute, modifier) -> attributeModifiers.add(new Pair<>(attribute, modifier))
			);

			MutableText effectText = getEffectText(effectType, amplifier);
			if (!effectInstance.isDurationBelow(20)) {
				effectText = Text.translatable(
						"potion.withDuration",
						effectText,
						StatusEffectUtil.getDurationText(effectInstance, durationMultiplier, tickRate)
				);
			}

			textConsumer.accept(effectText.formatted(effectType.value().getCategory().getFormatting()));
		}

		if (hasNoEffects) {
			textConsumer.accept(NONE_TEXT);
		}

		if (attributeModifiers.isEmpty()) {
			return;
		}

		textConsumer.accept(ScreenTexts.EMPTY);
		textConsumer.accept(Text.translatable("potion.whenDrank").formatted(Formatting.DARK_PURPLE));

		for (Pair<RegistryEntry<EntityAttribute>, EntityAttributeModifier> pair : attributeModifiers) {
			EntityAttributeModifier modifier = pair.getSecond();
			double rawValue = modifier.value();
			boolean isMultiplier = modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
					|| modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
			double displayValue = isMultiplier ? rawValue * 100.0 : rawValue;

			if (rawValue > 0.0) {
				textConsumer.accept(
						Text.translatable(
								"attribute.modifier.plus." + modifier.operation().getId(),
								AttributeModifiersComponent.DECIMAL_FORMAT.format(displayValue),
								Text.translatable(pair.getFirst().value().getTranslationKey())
						).formatted(Formatting.BLUE)
				);
			}
			else if (rawValue < 0.0) {
				textConsumer.accept(
						Text.translatable(
								"attribute.modifier.take." + modifier.operation().getId(),
								AttributeModifiersComponent.DECIMAL_FORMAT.format(-displayValue),
								Text.translatable(pair.getFirst().value().getTranslationKey())
						).formatted(Formatting.RED)
				);
			}
		}
	}

	public static MutableText getEffectText(RegistryEntry<StatusEffect> effect, int amplifier) {
		MutableText name = Text.translatable(effect.value().getTranslationKey());
		return amplifier > 0
				? Text.translatable("potion.withAmplifier", name, Text.translatable("potion.potency." + amplifier))
				: name;
	}

	@Override
	public void onConsume(World world, LivingEntity user, ItemStack stack, ConsumableComponent consumable) {
		apply(user, stack.getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F));
	}

	@Override
	public void appendTooltip(
			Item.TooltipContext context,
			Consumer<Text> textConsumer,
			TooltipType type,
			ComponentsAccess components
	) {
		buildTooltip(
				getEffects(),
				textConsumer,
				components.getOrDefault(DataComponentTypes.POTION_DURATION_SCALE, 1.0F),
				context.getUpdateTickRate()
		);
	}
}
