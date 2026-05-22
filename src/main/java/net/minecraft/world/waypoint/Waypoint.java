package net.minecraft.world.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Базовый интерфейс для всех вейпоинтов — маркеров позиции, отображаемых игрокам.
 * <p>
 * Вейпоинт может быть привязан к позиции, чанку или азимуту.
 * Конфигурация визуального представления хранится в {@link Config}.
 */
public interface Waypoint {

	/** Дальность отслеживания игрока по умолчанию (в блоках). */
	int DEFAULT_PLAYER_RANGE = 60000000;

	/**
	 * Модификатор атрибута, полностью отключающий передачу вейпоинта другим игрокам.
	 * Применяется через {@link #disableTracking(Item.Settings)}.
	 */
	EntityAttributeModifier DISABLE_TRACKING = new EntityAttributeModifier(
		Identifier.ofVanilla("waypoint_transmit_range_hide"),
		-1.0,
		EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	/**
	 * Добавляет к настройкам предмета модификатор, скрывающий вейпоинт владельца.
	 * Используется для предметов, надеваемых на голову (например, тыква).
	 */
	static Item.Settings disableTracking(Item.Settings settings) {
		return settings.component(
			DataComponentTypes.ATTRIBUTE_MODIFIERS,
			AttributeModifiersComponent.builder()
				.add(
					EntityAttributes.WAYPOINT_TRANSMIT_RANGE,
					DISABLE_TRACKING,
					AttributeModifierSlot.HEAD,
					AttributeModifiersComponent.Display.getHidden()
				)
				.build()
		);
	}

	/**
	 * Изменяемая конфигурация визуального представления вейпоинта.
	 * <p>
	 * Содержит стиль отображения и опциональный цвет. Может быть сериализована
	 * как через {@link Codec} (NBT/JSON), так и через {@link PacketCodec} (сеть).
	 */
	class Config {

		public static final Codec<Config> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				RegistryKey.createCodec(WaypointStyles.REGISTRY)
					.fieldOf("style")
					.forGetter(config -> config.style),
				Codecs.RGB.optionalFieldOf("color").forGetter(config -> config.color)
			).apply(instance, Config::new)
		);

		public static final PacketCodec<ByteBuf, Config> PACKET_CODEC = PacketCodec.tuple(
			RegistryKey.createPacketCodec(WaypointStyles.REGISTRY),
			config -> config.style,
			PacketCodecs.optional(PacketCodecs.RGB),
			config -> config.color,
			Config::new
		);

		public static final Config DEFAULT = new Config();

		public RegistryKey<WaypointStyle> style = WaypointStyles.DEFAULT;
		public Optional<Integer> color = Optional.empty();

		public Config() {
		}

		private Config(RegistryKey<WaypointStyle> style, Optional<Integer> color) {
			this.style = style;
			this.color = color;
		}

		public boolean hasCustomStyle() {
			return style != WaypointStyles.DEFAULT || color.isPresent();
		}

		/**
		 * Возвращает новый {@link Config} с цветом команды сущности, если цвет ещё не задан явно.
		 * <p>
		 * Если у команды нет цвета (значение {@code 0} — белый), подставляется
		 * нейтральный серый {@code -13619152}, чтобы избежать слияния с фоном.
		 */
		public Config withTeamColorOf(LivingEntity entity) {
			RegistryKey<WaypointStyle> resolvedStyle = style;
			Optional<Integer> resolvedColor = color
				.or(() -> Optional
					.ofNullable(entity.getScoreboardTeam())
					.map(team -> team.getColor().getColorValue())
					.map(teamColor -> teamColor == 0 ? -13619152 : teamColor)
				);

			return resolvedStyle == style && resolvedColor.isEmpty()
				? this
				: new Config(resolvedStyle, resolvedColor);
		}

		public void copyFrom(Config other) {
			color = other.color;
			style = other.style;
		}

		private RegistryKey<WaypointStyle> getStyle() {
			return style;
		}
	}
}
