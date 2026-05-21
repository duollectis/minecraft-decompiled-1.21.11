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
 * {@code Waypoint}.
 */
public interface Waypoint {

	int DEFAULT_PLAYER_RANGE = 60000000;

	EntityAttributeModifier DISABLE_TRACKING = new EntityAttributeModifier(
			Identifier.ofVanilla("waypoint_transmit_range_hide"),
			-1.0,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

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
	 * {@code Config}.
	 */
	public static class Config {

		public static final Codec<Waypoint.Config> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    RegistryKey
								                    .createCodec(WaypointStyles.REGISTRY)
								                    .fieldOf("style")
								                    .forGetter(config -> config.style),
						                    Codecs.RGB.optionalFieldOf("color").forGetter(config -> config.color)
				                    )
				                    .apply(instance, Waypoint.Config::new)
		);
		public static final PacketCodec<ByteBuf, Waypoint.Config> PACKET_CODEC = PacketCodec.tuple(
				RegistryKey.createPacketCodec(WaypointStyles.REGISTRY),
				config -> config.style,
				PacketCodecs.optional(PacketCodecs.RGB),
				config -> config.color,
				Waypoint.Config::new
		);
		public static final Waypoint.Config DEFAULT = new Waypoint.Config();
		public RegistryKey<WaypointStyle> style = WaypointStyles.DEFAULT;
		public Optional<Integer> color = Optional.empty();

		public Config() {
		}

		private Config(RegistryKey<WaypointStyle> style, Optional<Integer> color) {
			this.style = style;
			this.color = color;
		}

		public boolean hasCustomStyle() {
			return this.style != WaypointStyles.DEFAULT || this.color.isPresent();
		}

		public Waypoint.Config withTeamColorOf(LivingEntity entity) {
			RegistryKey<WaypointStyle> registryKey = this.getStyle();
			Optional<Integer> optional = this.color
					.or(() -> Optional
							.ofNullable(entity.getScoreboardTeam())
							.map(team -> team.getColor().getColorValue())
							.map(color -> color == 0 ? -13619152 : color));
			return registryKey == this.style && optional.isEmpty() ? this : new Waypoint.Config(registryKey, optional);
		}

		public void copyFrom(Waypoint.Config config) {
			this.color = config.color;
			this.style = config.style;
		}

		private RegistryKey<WaypointStyle> getStyle() {
			return this.style != WaypointStyles.DEFAULT ? this.style : WaypointStyles.DEFAULT;
		}
	}
}
