package net.minecraft.entity.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

/**
 * {@code EntityAttributeModifier}.
 */
public record EntityAttributeModifier(Identifier id, double value, EntityAttributeModifier.Operation operation) {

	public static final MapCodec<EntityAttributeModifier> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Identifier.CODEC.fieldOf("id").forGetter(EntityAttributeModifier::id),
					                    Codec.DOUBLE.fieldOf("amount").forGetter(EntityAttributeModifier::value),
					                    EntityAttributeModifier.Operation.CODEC
							                    .fieldOf("operation")
							                    .forGetter(EntityAttributeModifier::operation)
			                    )
			                    .apply(instance, EntityAttributeModifier::new)
	);
	public static final Codec<EntityAttributeModifier> CODEC = MAP_CODEC.codec();
	public static final PacketCodec<ByteBuf, EntityAttributeModifier> PACKET_CODEC = PacketCodec.tuple(
			Identifier.PACKET_CODEC,
			EntityAttributeModifier::id,
			PacketCodecs.DOUBLE,
			EntityAttributeModifier::value,
			EntityAttributeModifier.Operation.PACKET_CODEC,
			EntityAttributeModifier::operation,
			EntityAttributeModifier::new
	);

	public boolean idMatches(Identifier id) {
		return id.equals(this.id);
	}

	/**
	 * {@code Operation}.
	 */
	public static enum Operation implements StringIdentifiable {
		ADD_VALUE("add_value", 0),
		ADD_MULTIPLIED_BASE("add_multiplied_base", 1),
		ADD_MULTIPLIED_TOTAL("add_multiplied_total", 2);

		public static final IntFunction<EntityAttributeModifier.Operation>
				ID_TO_VALUE =
				ValueLists.createIndexToValueFunction(
						EntityAttributeModifier.Operation::getId, values(), ValueLists.OutOfBoundsHandling.ZERO
				);
		public static final PacketCodec<ByteBuf, EntityAttributeModifier.Operation> PACKET_CODEC = PacketCodecs.indexed(
				ID_TO_VALUE, EntityAttributeModifier.Operation::getId
		);
		public static final Codec<EntityAttributeModifier.Operation>
				CODEC =
				StringIdentifiable.createCodec(EntityAttributeModifier.Operation::values);
		private final String name;
		private final int id;

		private Operation(final String name, final int id) {
			this.name = name;
			this.id = id;
		}

		public int getId() {
			return this.id;
		}

		@Override
		public String asString() {
			return this.name;
		}
	}
}
