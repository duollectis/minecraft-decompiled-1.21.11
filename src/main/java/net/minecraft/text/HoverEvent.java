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
 * {@code HoverEvent}.
 */
public interface HoverEvent {

	Codec<HoverEvent> CODEC = HoverEvent.Action.CODEC.dispatch("action", HoverEvent::getAction, action -> action.codec);

	HoverEvent.Action getAction();

	/**
	 * {@code Action}.
	 */
	public static enum Action implements StringIdentifiable {
		SHOW_TEXT("show_text", true, HoverEvent.ShowText.CODEC),
		SHOW_ITEM("show_item", true, HoverEvent.ShowItem.CODEC),
		SHOW_ENTITY("show_entity", true, HoverEvent.ShowEntity.CODEC);

		public static final Codec<HoverEvent.Action>
				UNVALIDATED_CODEC =
				StringIdentifiable.createBasicCodec(HoverEvent.Action::values);
		public static final Codec<HoverEvent.Action> CODEC = UNVALIDATED_CODEC.validate(HoverEvent.Action::validate);
		private final String name;
		private final boolean parsable;
		final MapCodec<? extends HoverEvent> codec;

		private Action(final String name, final boolean parsable, final MapCodec<? extends HoverEvent> codec) {
			this.name = name;
			this.parsable = parsable;
			this.codec = codec;
		}

		public boolean isParsable() {
			return this.parsable;
		}

		@Override
		public String asString() {
			return this.name;
		}

		@Override
		public String toString() {
			return "<action " + this.name + ">";
		}

		private static DataResult<HoverEvent.Action> validate(HoverEvent.Action action) {
			return !action.isParsable() ? DataResult.error(() -> "Action not allowed: " + action)
			                            : DataResult.success(action, Lifecycle.stable());
		}
	}

	/**
	 * {@code EntityContent}.
	 */
	public static class EntityContent {

		public static final MapCodec<HoverEvent.EntityContent> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Registries.ENTITY_TYPE.getCodec().fieldOf("id").forGetter(content -> content.entityType),
						                    Uuids.STRICT_CODEC.fieldOf("uuid").forGetter(content -> content.uuid),
						                    TextCodecs.CODEC.optionalFieldOf("name").forGetter(content -> content.name)
				                    )
				                    .apply(instance, HoverEvent.EntityContent::new)
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
		 * As tooltip.
		 *
		 * @return List — результат операции
		 */
		public List<Text> asTooltip() {
			if (this.tooltip == null) {
				this.tooltip = new ArrayList<>();
				this.name.ifPresent(this.tooltip::add);
				this.tooltip.add(Text.translatable("gui.entity_tooltip.type", this.entityType.getName()));
				this.tooltip.add(Text.literal(this.uuid.toString()));
			}

			return this.tooltip;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			else if (o != null && this.getClass() == o.getClass()) {
				HoverEvent.EntityContent entityContent = (HoverEvent.EntityContent) o;
				return this.entityType.equals(entityContent.entityType) && this.uuid.equals(entityContent.uuid)
						&& this.name.equals(entityContent.name);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int i = this.entityType.hashCode();
			i = 31 * i + this.uuid.hashCode();
			return 31 * i + this.name.hashCode();
		}
	}

	/**
	 * {@code ShowEntity}.
	 */
	public record ShowEntity(HoverEvent.EntityContent entity) implements HoverEvent {

		public static final MapCodec<HoverEvent.ShowEntity> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(HoverEvent.EntityContent.CODEC.forGetter(HoverEvent.ShowEntity::entity))
						.apply(instance, HoverEvent.ShowEntity::new)
		);

		@Override
		public HoverEvent.Action getAction() {
			return HoverEvent.Action.SHOW_ENTITY;
		}
	}

	/**
	 * {@code ShowItem}.
	 */
	public record ShowItem(ItemStack item) implements HoverEvent {

		public static final MapCodec<HoverEvent.ShowItem>
				CODEC =
				ItemStack.MAP_CODEC.xmap(HoverEvent.ShowItem::new, HoverEvent.ShowItem::item);

		public ShowItem(ItemStack item) {
			item = item.copy();
			this.item = item;
		}

		@Override
		public HoverEvent.Action getAction() {
			return HoverEvent.Action.SHOW_ITEM;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof HoverEvent.ShowItem showItem && ItemStack.areEqual(this.item, showItem.item);
		}

		@Override
		public int hashCode() {
			return ItemStack.hashCode(this.item);
		}
	}

	/**
	 * {@code ShowText}.
	 */
	public record ShowText(Text value) implements HoverEvent {

		public static final MapCodec<HoverEvent.ShowText> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(TextCodecs.CODEC.fieldOf("value").forGetter(HoverEvent.ShowText::value))
						.apply(instance, HoverEvent.ShowText::new)
		);

		@Override
		public HoverEvent.Action getAction() {
			return HoverEvent.Action.SHOW_TEXT;
		}
	}
}
