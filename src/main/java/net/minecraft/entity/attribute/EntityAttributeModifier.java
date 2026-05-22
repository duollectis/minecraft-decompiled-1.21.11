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
 * Неизменяемый модификатор атрибута сущности. Описывает, как именно изменяется значение атрибута:
 * через прибавку к базе, умножение базы или умножение итогового значения.
 */
public record EntityAttributeModifier(Identifier id, double value, Operation operation) {

	public static final MapCodec<EntityAttributeModifier> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Identifier.CODEC.fieldOf("id").forGetter(EntityAttributeModifier::id),
					Codec.DOUBLE.fieldOf("amount").forGetter(EntityAttributeModifier::value),
					Operation.CODEC.fieldOf("operation").forGetter(EntityAttributeModifier::operation)
			).apply(instance, EntityAttributeModifier::new)
	);
	public static final Codec<EntityAttributeModifier> CODEC = MAP_CODEC.codec();
	public static final PacketCodec<ByteBuf, EntityAttributeModifier> PACKET_CODEC = PacketCodec.tuple(
			Identifier.PACKET_CODEC, EntityAttributeModifier::id,
			PacketCodecs.DOUBLE, EntityAttributeModifier::value,
			Operation.PACKET_CODEC, EntityAttributeModifier::operation,
			EntityAttributeModifier::new
	);

	public boolean idMatches(Identifier otherId) {
		return otherId.equals(id);
	}

	/**
	 * Тип операции, применяемой модификатором к итоговому значению атрибута.
	 * <ul>
	 *   <li>{@link #ADD_VALUE} — прибавляет {@code value} к базовому значению.</li>
	 *   <li>{@link #ADD_MULTIPLIED_BASE} — прибавляет {@code base * value} к накопленной сумме.</li>
	 *   <li>{@link #ADD_MULTIPLIED_TOTAL} — умножает текущий итог на {@code (1 + value)}.</li>
	 * </ul>
	 */
	public enum Operation implements StringIdentifiable {
		ADD_VALUE("add_value", 0),
		ADD_MULTIPLIED_BASE("add_multiplied_base", 1),
		ADD_MULTIPLIED_TOTAL("add_multiplied_total", 2);

		public static final IntFunction<Operation> ID_TO_VALUE = ValueLists.createIndexToValueFunction(
				Operation::getId, values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		public static final PacketCodec<ByteBuf, Operation> PACKET_CODEC =
				PacketCodecs.indexed(ID_TO_VALUE, Operation::getId);
		public static final Codec<Operation> CODEC = StringIdentifiable.createCodec(Operation::values);

		private final String name;
		private final int id;

		Operation(String name, int id) {
			this.name = name;
			this.id = id;
		}

		public int getId() {
			return id;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
