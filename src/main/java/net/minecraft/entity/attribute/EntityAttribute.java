package net.minecraft.entity.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;

/**
 * Базовый класс атрибута сущности. Определяет числовую характеристику (здоровье, скорость, урон и т.д.),
 * которая может быть изменена через {@link EntityAttributeModifier}.
 * <p>
 * Подклассы (например, {@link ClampedEntityAttribute}) могут ограничивать допустимый диапазон значений.
 */
public class EntityAttribute {

	public static final Codec<RegistryEntry<EntityAttribute>> CODEC = Registries.ATTRIBUTE.getEntryCodec();
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<EntityAttribute>> PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.ATTRIBUTE);

	private final double fallback;
	private final String translationKey;
	private boolean tracked;
	private Category category = Category.POSITIVE;

	protected EntityAttribute(String translationKey, double fallback) {
		this.fallback = fallback;
		this.translationKey = translationKey;
	}

	public double getDefaultValue() {
		return fallback;
	}

	public boolean isTracked() {
		return tracked;
	}

	public EntityAttribute setTracked(boolean tracked) {
		this.tracked = tracked;
		return this;
	}

	public EntityAttribute setCategory(Category category) {
		this.category = category;
		return this;
	}

	/**
	 * Ограничивает значение атрибута допустимым диапазоном.
	 * Базовая реализация не накладывает ограничений — переопределяется в {@link ClampedEntityAttribute}.
	 */
	public double clamp(double value) {
		return value;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	public Formatting getFormatting(boolean addition) {
		return category.getFormatting(addition);
	}

	/**
	 * Семантическая категория атрибута, определяющая цвет отображения в подсказке предмета.
	 * {@code POSITIVE} — полезный атрибут, {@code NEGATIVE} — вредный, {@code NEUTRAL} — нейтральный.
	 */
	public enum Category {
		POSITIVE,
		NEUTRAL,
		NEGATIVE;

		public Formatting getFormatting(boolean addition) {
			return switch (this) {
				case POSITIVE -> addition ? Formatting.BLUE : Formatting.RED;
				case NEUTRAL -> Formatting.GRAY;
				case NEGATIVE -> addition ? Formatting.RED : Formatting.BLUE;
			};
		}
	}
}
