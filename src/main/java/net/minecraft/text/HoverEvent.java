package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Uuids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Событие, срабатывающее при наведении курсора на текст.
 * Каждая реализация соответствует конкретному типу всплывающей подсказки.
 */
public interface HoverEvent {

	Codec<HoverEvent> CODEC = Action.CODEC.dispatch("action", HoverEvent::getAction, action -> action.codec);

	Action getAction();

	/**
	 * Перечисление поддерживаемых типов hover-событий.
	 * Флаг {@code parsable} определяет, может ли действие быть задано в тексте игроком.
	 */
	enum Action implements StringIdentifiable {
		SHOW_TEXT("show_text", true, ShowText.CODEC),
		SHOW_ITEM("show_item", true, ShowItem.CODEC),
		SHOW_ENTITY("show_entity", true, ShowEntity.CODEC);

		public static final Codec<Action> UNVALIDATED_CODEC =
			StringIdentifiable.createBasicCodec(Action::values);
		public static final Codec<Action> CODEC = UNVALIDATED_CODEC.validate(Action::validate);

		private final String name;
		private final boolean parsable;
		final MapCodec<? extends HoverEvent> codec;

		Action(String name, boolean parsable, MapCodec<? extends HoverEvent> codec) {
			this.name = name;
			this.parsable = parsable;
			this.codec = codec;
		}

		public boolean isParsable() {
			return parsable;
		}

		@Override
		public String asString() {
			return name;
		}

		@Override
		public String toString() {
			return "<action " + name + ">";
		}

		private static DataResult<Action> validate(Action action) {
			return action.isParsable()
				? DataResult.success(action, Lifecycle.stable())
				: DataResult.error(() -> "Action not allowed: " + action);
		}
	}

	/**
	 * Данные о сущности для отображения во всплывающей подсказке.
	 * Содержит тип, UUID и опциональное имя сущности.
	 * Список строк тултипа кешируется лениво при первом обращении.
	 */
	class EntityContent {

		public static final MapCodec<EntityContent> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Registries.ENTITY_TYPE.getCodec().fieldOf("id").forGetter(content -> content.entityType),
				Uuids.STRICT_CODEC.fieldOf("uuid").forGetter(content -> content.uuid),
				TextCodecs.CODEC.optionalFieldOf("name").forGetter(content -> content.name)
			).apply(instance, EntityContent::new)
		);

		public final EntityType<?> entityType;
		public final UUID uuid;
		public final Optional<Text> name;
		private @Nullable List<Text> tooltip;

		public EntityContent(EntityType<?> entityType, UUID uuid, @Nullable Text name) {
			this(entityType, uuid, Optional.ofNullable(name));
		}

		public EntityContent(EntityType<?> entityType, UUID uuid, Optional<Text> name) {
			this.entityType = entityType;
			this.uuid = uuid;
			this.name = name;
		}

		/**
		 * Возвращает список строк тултипа: имя (если есть), тип сущности и UUID.
		 * Результат кешируется после первого вызова.
		 */
		public List<Text> asTooltip() {
			if (tooltip == null) {
				tooltip = new ArrayList<>();
				name.ifPresent(tooltip::add);
				tooltip.add(Text.translatable("gui.entity_tooltip.type", entityType.getName()));
				tooltip.add(Text.literal(uuid.toString()));
			}

			return tooltip;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			EntityContent other = (EntityContent) o;
			return entityType.equals(other.entityType)
				&& uuid.equals(other.uuid)
				&& name.equals(other.name);
		}

		@Override
		public int hashCode() {
			int hash = entityType.hashCode();
			hash = 31 * hash + uuid.hashCode();
			return 31 * hash + name.hashCode();
		}
	}

	/**
	 * Показывает информацию о сущности при наведении.
	 */
	record ShowEntity(EntityContent entity) implements HoverEvent {

		public static final MapCodec<ShowEntity> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(EntityContent.CODEC.forGetter(ShowEntity::entity))
				.apply(instance, ShowEntity::new)
		);

		@Override
		public Action getAction() {
			return Action.SHOW_ENTITY;
		}
	}

	/**
	 * Показывает тултип предмета при наведении.
	 * Стек предмета копируется при создании для обеспечения иммутабельности.
	 */
	record ShowItem(ItemStack item) implements HoverEvent {

		public static final MapCodec<ShowItem> CODEC =
			ItemStack.MAP_CODEC.xmap(stack -> new ShowItem(stack.copy()), ShowItem::item);

		public ShowItem {
			item = item.copy();
		}

		@Override
		public Action getAction() {
			return Action.SHOW_ITEM;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ShowItem other && ItemStack.areEqual(item, other.item);
		}

		@Override
		public int hashCode() {
			return ItemStack.hashCode(item);
		}
	}

	/**
	 * Показывает произвольный текст при наведении.
	 */
	record ShowText(Text value) implements HoverEvent {

		public static final MapCodec<ShowText> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(TextCodecs.CODEC.fieldOf("value").forGetter(ShowText::value))
				.apply(instance, ShowText::new)
		);

		@Override
		public Action getAction() {
			return Action.SHOW_TEXT;
		}
	}
}
