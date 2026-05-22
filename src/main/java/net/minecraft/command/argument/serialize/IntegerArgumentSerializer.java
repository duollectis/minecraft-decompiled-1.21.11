package net.minecraft.command.argument.serialize;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.network.PacketByteBuf;

/**
 * Сериализатор аргумента {@link IntegerArgumentType} для передачи по сети и записи в JSON.
 * Передаёт минимальное и максимальное допустимые значения через битовый флаг присутствия.
 */
public class IntegerArgumentSerializer implements ArgumentSerializer<IntegerArgumentType, IntegerArgumentSerializer.Properties> {

	public void writePacket(IntegerArgumentSerializer.Properties properties, PacketByteBuf packetByteBuf) {
		boolean hasMin = properties.min != Integer.MIN_VALUE;
		boolean hasMax = properties.max != Integer.MAX_VALUE;

		packetByteBuf.writeByte(ArgumentHelper.getMinMaxFlag(hasMin, hasMax));

		if (hasMin) {
			packetByteBuf.writeInt(properties.min);
		}

		if (hasMax) {
			packetByteBuf.writeInt(properties.max);
		}
	}

	public IntegerArgumentSerializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
		byte flags = packetByteBuf.readByte();
		int min = ArgumentHelper.hasMinFlag(flags) ? packetByteBuf.readInt() : Integer.MIN_VALUE;
		int max = ArgumentHelper.hasMaxFlag(flags) ? packetByteBuf.readInt() : Integer.MAX_VALUE;

		return new IntegerArgumentSerializer.Properties(min, max);
	}

	public void writeJson(IntegerArgumentSerializer.Properties properties, JsonObject jsonObject) {
		if (properties.min != Integer.MIN_VALUE) {
			jsonObject.addProperty("min", properties.min);
		}

		if (properties.max != Integer.MAX_VALUE) {
			jsonObject.addProperty("max", properties.max);
		}
	}

	public IntegerArgumentSerializer.Properties getArgumentTypeProperties(IntegerArgumentType integerArgumentType) {
		return new IntegerArgumentSerializer.Properties(
				integerArgumentType.getMinimum(),
				integerArgumentType.getMaximum()
		);
	}

	/**
	 * Свойства сериализатора: хранит диапазон допустимых значений.
	 */
	public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<IntegerArgumentType> {

		final int min;
		final int max;

		Properties(final int min, final int max) {
			this.min = min;
			this.max = max;
		}

		public IntegerArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
			return IntegerArgumentType.integer(this.min, this.max);
		}

		@Override
		public ArgumentSerializer<IntegerArgumentType, ?> getSerializer() {
			return IntegerArgumentSerializer.this;
		}
	}
}
