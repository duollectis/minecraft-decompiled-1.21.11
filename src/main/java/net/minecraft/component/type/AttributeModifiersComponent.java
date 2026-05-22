package net.minecraft.component.type;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
	 * Компонент модификаторов атрибутов предмета.
	 * Хранит список записей, каждая из которых связывает атрибут с модификатором и слотом экипировки.
	 */
public record AttributeModifiersComponent(List<AttributeModifiersComponent.Entry> modifiers) {

	public static final AttributeModifiersComponent DEFAULT = new AttributeModifiersComponent(List.of());
	public static final Codec<AttributeModifiersComponent> CODEC = AttributeModifiersComponent.Entry.CODEC
			.listOf()
			.xmap(AttributeModifiersComponent::new, AttributeModifiersComponent::modifiers);
	public static final PacketCodec<RegistryByteBuf, AttributeModifiersComponent> PACKET_CODEC = PacketCodec.tuple(
			AttributeModifiersComponent.Entry.PACKET_CODEC.collect(PacketCodecs.toList()),
			AttributeModifiersComponent::modifiers,
			AttributeModifiersComponent::new
	);
	public static final DecimalFormat
			DECIMAL_FORMAT =
			new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

	public static AttributeModifiersComponent.Builder builder() {
		return new AttributeModifiersComponent.Builder();
	}

	public AttributeModifiersComponent with(
			RegistryEntry<EntityAttribute> attribute,
			EntityAttributeModifier modifier,
			AttributeModifierSlot slot
	) {
		ImmutableList.Builder<AttributeModifiersComponent.Entry> builder = ImmutableList.builderWithExpectedSize(
				modifiers.size() + 1
		);

		for (AttributeModifiersComponent.Entry entry : modifiers) {
			if (!entry.matches(attribute, modifier.id())) {
				builder.add(entry);
			}
		}

		builder.add(new AttributeModifiersComponent.Entry(attribute, modifier, slot));
		return new AttributeModifiersComponent(builder.build());
	}

	public void applyModifiers(
			AttributeModifierSlot slot,
			TriConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier, AttributeModifiersComponent.Display> attributeConsumer
	) {
		for (AttributeModifiersComponent.Entry entry : modifiers) {
			if (entry.slot.equals(slot)) {
				attributeConsumer.accept(entry.attribute, entry.modifier, entry.display);
			}
		}
	}

	public void applyModifiers(
			AttributeModifierSlot slot,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeConsumer
	) {
		for (AttributeModifiersComponent.Entry entry : modifiers) {
			if (entry.slot.equals(slot)) {
				attributeConsumer.accept(entry.attribute, entry.modifier);
			}
		}
	}

	public void applyModifiers(
			EquipmentSlot slot,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeConsumer
	) {
		for (AttributeModifiersComponent.Entry entry : modifiers) {
			if (entry.slot.matches(slot)) {
				attributeConsumer.accept(entry.attribute, entry.modifier);
			}
		}
	}

	/**
		 * Последовательно применяет все модификаторы атрибута для указанного слота к базовому значению.
		 * Порядок применения: ADD_VALUE → ADD_MULTIPLIED_BASE → ADD_MULTIPLIED_TOTAL.
		 *
		 * @param attribute целевой атрибут
		 * @param base      базовое значение атрибута
		 * @param slot      слот экипировки
		 * @return итоговое значение после применения всех модификаторов
		 */
	public double applyOperations(RegistryEntry<EntityAttribute> attribute, double base, EquipmentSlot slot) {
		double result = base;

		for (AttributeModifiersComponent.Entry entry : modifiers) {
			if (entry.slot.matches(slot) && entry.attribute == attribute) {
				double modifierValue = entry.modifier.value();

				result += switch (entry.modifier.operation()) {
					case ADD_VALUE -> modifierValue;
					case ADD_MULTIPLIED_BASE -> modifierValue * base;
					case ADD_MULTIPLIED_TOTAL -> modifierValue * result;
				};
			}
		}

		return result;
	}

	public static class Builder {

		private final ImmutableList.Builder<AttributeModifiersComponent.Entry> entries = ImmutableList.builder();

		Builder() {
		}

		public AttributeModifiersComponent.Builder add(
				RegistryEntry<EntityAttribute> attribute,
				EntityAttributeModifier modifier,
				AttributeModifierSlot slot
		) {
			entries.add(new AttributeModifiersComponent.Entry(attribute, modifier, slot));
			return this;
		}

		public AttributeModifiersComponent.Builder add(
				RegistryEntry<EntityAttribute> attribute,
				EntityAttributeModifier modifier,
				AttributeModifierSlot slot,
				AttributeModifiersComponent.Display display
		) {
			entries.add(new AttributeModifiersComponent.Entry(attribute, modifier, slot, display));
			return this;
		}

		public AttributeModifiersComponent build() {
			return new AttributeModifiersComponent(entries.build());
		}
	}

	/**
		 * Стратегия отображения модификатора атрибута в тултипе предмета.
		 * Определяет, как именно будет показан модификатор: стандартно, скрыто или с переопределённым текстом.
		 */
	public interface Display {

		Codec<AttributeModifiersComponent.Display> CODEC = AttributeModifiersComponent.Display.Type.CODEC
				.dispatch("type", AttributeModifiersComponent.Display::getType, type -> type.codec);

		PacketCodec<RegistryByteBuf, AttributeModifiersComponent.Display>
				PACKET_CODEC =
				AttributeModifiersComponent.Display.Type.PACKET_CODEC
						.<RegistryByteBuf>cast()
						.dispatch(
								AttributeModifiersComponent.Display::getType,
								AttributeModifiersComponent.Display.Type::getPacketCodec
						);

		static AttributeModifiersComponent.Display getDefault() {
			return AttributeModifiersComponent.Display.Default.INSTANCE;
		}

		static AttributeModifiersComponent.Display getHidden() {
			return AttributeModifiersComponent.Display.Hidden.INSTANCE;
		}

		static AttributeModifiersComponent.Display createOverride(Text text) {
			return new AttributeModifiersComponent.Display.OverrideDisplay(text);
		}

		AttributeModifiersComponent.Display.Type getType();

		void addTooltip(
				Consumer<Text> textConsumer,
				@Nullable PlayerEntity player,
				RegistryEntry<EntityAttribute> attribute,
				EntityAttributeModifier modifier
		);

		public record Default() implements AttributeModifiersComponent.Display {

			static final AttributeModifiersComponent.Display.Default
					INSTANCE =
					new AttributeModifiersComponent.Display.Default();
			static final MapCodec<AttributeModifiersComponent.Display.Default> CODEC = MapCodec.unit(INSTANCE);
			static final PacketCodec<RegistryByteBuf, AttributeModifiersComponent.Display.Default>
					PACKET_CODEC =
					PacketCodec.unit(INSTANCE);

			@Override
			public AttributeModifiersComponent.Display.Type getType() {
				return AttributeModifiersComponent.Display.Type.DEFAULT;
			}

			@Override
			public void addTooltip(
					Consumer<Text> textConsumer,
					@Nullable PlayerEntity player,
					RegistryEntry<EntityAttribute> attribute,
					EntityAttributeModifier modifier
			) {
				double value = modifier.value();
				boolean isBaseModifier = false;

				if (player != null) {
					if (modifier.idMatches(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID)) {
						value += player.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE);
						isBaseModifier = true;
					} else if (modifier.idMatches(Item.BASE_ATTACK_SPEED_MODIFIER_ID)) {
						value += player.getAttributeBaseValue(EntityAttributes.ATTACK_SPEED);
						isBaseModifier = true;
					}
				}

				double displayValue;
				if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
						|| modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
					displayValue = value * 100.0;
				} else if (attribute.matches(EntityAttributes.KNOCKBACK_RESISTANCE)) {
					displayValue = value * 10.0;
				} else {
					displayValue = value;
				}

				if (isBaseModifier) {
					textConsumer.accept(
							ScreenTexts.space()
										.append(
												Text.translatable(
														"attribute.modifier.equals." + modifier.operation().getId(),
														DECIMAL_FORMAT.format(displayValue),
														Text.translatable(attribute.value().getTranslationKey())
												)
										)
										.formatted(Formatting.DARK_GREEN)
					);
				} else if (value > 0.0) {
					textConsumer.accept(
							Text.translatable(
										"attribute.modifier.plus." + modifier.operation().getId(),
										DECIMAL_FORMAT.format(displayValue),
										Text.translatable(attribute.value().getTranslationKey())
								)
								.formatted(attribute.value().getFormatting(true))
					);
				} else if (value < 0.0) {
					textConsumer.accept(
							Text.translatable(
										"attribute.modifier.take." + modifier.operation().getId(),
										DECIMAL_FORMAT.format(-displayValue),
										Text.translatable(attribute.value().getTranslationKey())
								)
								.formatted(attribute.value().getFormatting(false))
					);
				}
			}
		}

		public record Hidden() implements AttributeModifiersComponent.Display {

			static final AttributeModifiersComponent.Display.Hidden
					INSTANCE =
					new AttributeModifiersComponent.Display.Hidden();
			static final MapCodec<AttributeModifiersComponent.Display.Hidden> CODEC = MapCodec.unit(INSTANCE);
			static final PacketCodec<RegistryByteBuf, AttributeModifiersComponent.Display.Hidden>
					PACKET_CODEC =
					PacketCodec.unit(INSTANCE);

			@Override
			public AttributeModifiersComponent.Display.Type getType() {
				return AttributeModifiersComponent.Display.Type.HIDDEN;
			}

			@Override
			public void addTooltip(
					Consumer<Text> textConsumer,
					@Nullable PlayerEntity player,
					RegistryEntry<EntityAttribute> attribute,
					EntityAttributeModifier modifier
			) {
			}
		}

		public record OverrideDisplay(Text value) implements AttributeModifiersComponent.Display {

			static final MapCodec<AttributeModifiersComponent.Display.OverrideDisplay>
					CODEC =
					RecordCodecBuilder.mapCodec(
							instance -> instance
									.group(TextCodecs.CODEC
											.fieldOf("value")
											.forGetter(AttributeModifiersComponent.Display.OverrideDisplay::value))
									.apply(instance, AttributeModifiersComponent.Display.OverrideDisplay::new)
					);
			static final PacketCodec<RegistryByteBuf, AttributeModifiersComponent.Display.OverrideDisplay>
					PACKET_CODEC =
					PacketCodec.tuple(
							TextCodecs.REGISTRY_PACKET_CODEC,
							AttributeModifiersComponent.Display.OverrideDisplay::value,
							AttributeModifiersComponent.Display.OverrideDisplay::new
					);

			@Override
			public AttributeModifiersComponent.Display.Type getType() {
				return AttributeModifiersComponent.Display.Type.OVERRIDE;
			}

			@Override
			public void addTooltip(
					Consumer<Text> textConsumer,
					@Nullable PlayerEntity player,
					RegistryEntry<EntityAttribute> attribute,
					EntityAttributeModifier modifier
			) {
				textConsumer.accept(value);
			}
		}

		public enum Type implements StringIdentifiable {
			DEFAULT(
					"default",
					0,
					AttributeModifiersComponent.Display.Default.CODEC,
					AttributeModifiersComponent.Display.Default.PACKET_CODEC
			),
			HIDDEN(
					"hidden",
					1,
					AttributeModifiersComponent.Display.Hidden.CODEC,
					AttributeModifiersComponent.Display.Hidden.PACKET_CODEC
			),
			OVERRIDE(
					"override",
					2,
					AttributeModifiersComponent.Display.OverrideDisplay.CODEC,
					AttributeModifiersComponent.Display.OverrideDisplay.PACKET_CODEC
			);

			static final Codec<AttributeModifiersComponent.Display.Type>
					CODEC =
					StringIdentifiable.createCodec(AttributeModifiersComponent.Display.Type::values);
			private static final IntFunction<AttributeModifiersComponent.Display.Type>
					INDEX_MAPPER =
					ValueLists.createIndexToValueFunction(
							AttributeModifiersComponent.Display.Type::getIndex,
							values(),
							ValueLists.OutOfBoundsHandling.ZERO
					);
			static final PacketCodec<ByteBuf, AttributeModifiersComponent.Display.Type>
					PACKET_CODEC =
					PacketCodecs.indexed(
							INDEX_MAPPER, AttributeModifiersComponent.Display.Type::getIndex
					);
			private final String id;
			private final int index;
			final MapCodec<? extends AttributeModifiersComponent.Display> codec;
			private final PacketCodec<RegistryByteBuf, ? extends AttributeModifiersComponent.Display> packetCodec;

			private Type(
					final String id,
					final int index,
					final MapCodec<? extends AttributeModifiersComponent.Display> codec,
					final PacketCodec<RegistryByteBuf, ? extends AttributeModifiersComponent.Display> packetCodec
			) {
				this.id = id;
				this.index = index;
				this.codec = codec;
				this.packetCodec = packetCodec;
			}

			@Override
			public String asString() {
				return this.id;
			}

			private int getIndex() {
				return this.index;
			}

			private PacketCodec<RegistryByteBuf, ? extends AttributeModifiersComponent.Display> getPacketCodec() {
				return this.packetCodec;
			}
		}
	}

	public record Entry(
			RegistryEntry<EntityAttribute> attribute,
			EntityAttributeModifier modifier,
			AttributeModifierSlot slot,
			AttributeModifiersComponent.Display display
	) {

		public static final Codec<AttributeModifiersComponent.Entry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
											EntityAttribute.CODEC.fieldOf("type").forGetter(AttributeModifiersComponent.Entry::attribute),
											EntityAttributeModifier.MAP_CODEC.forGetter(AttributeModifiersComponent.Entry::modifier),
											AttributeModifierSlot.CODEC
													.optionalFieldOf("slot", AttributeModifierSlot.ANY)
													.forGetter(AttributeModifiersComponent.Entry::slot),
											AttributeModifiersComponent.Display.CODEC
													.optionalFieldOf("display", AttributeModifiersComponent.Display.Default.INSTANCE)
													.forGetter(AttributeModifiersComponent.Entry::display)
									)
									.apply(instance, AttributeModifiersComponent.Entry::new)
		);
		public static final PacketCodec<RegistryByteBuf, AttributeModifiersComponent.Entry>
				PACKET_CODEC =
				PacketCodec.tuple(
						EntityAttribute.PACKET_CODEC,
						AttributeModifiersComponent.Entry::attribute,
						EntityAttributeModifier.PACKET_CODEC,
						AttributeModifiersComponent.Entry::modifier,
						AttributeModifierSlot.PACKET_CODEC,
						AttributeModifiersComponent.Entry::slot,
						AttributeModifiersComponent.Display.PACKET_CODEC,
						AttributeModifiersComponent.Entry::display,
						AttributeModifiersComponent.Entry::new
				);

		public Entry(
				RegistryEntry<EntityAttribute> attribute,
				EntityAttributeModifier modifier,
				AttributeModifierSlot slot
		) {
			this(attribute, modifier, slot, AttributeModifiersComponent.Display.getDefault());
		}

		public boolean matches(RegistryEntry<EntityAttribute> attribute, Identifier modifierId) {
			return attribute.equals(this.attribute) && modifier.idMatches(modifierId);
		}
	}
}
