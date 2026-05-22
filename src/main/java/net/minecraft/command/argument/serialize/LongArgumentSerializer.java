package net.minecraft.command.argument.serialize;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.network.PacketByteBuf;

/**
 * Сериализатор аргумента {@link LongArgumentType} для передачи по сети и записи в JSON.
 * Передаёт минимальное и максимальное допустимые значения через битовый флаг присутствия.
 */
public class LongArgumentSerializer implements ArgumentSerializer<LongArgumentType, LongArgumentSerializer.Properties> {

	public void writePacket(LongArgumentSerializer.Properties properties, PacketByteBuf packetByteBuf) {
		boolean hasMin = properties.min != Long.MIN_VALUE;
		boolean hasMax = properties.max != Long.MAX_VALUE;

		packetByteBuf.writeByte(ArgumentHelper.getMinMaxFlag(hasMin, hasMax));

		if (hasMin) {
			packetByteBuf.writeLong(properties.min);
		}

		if (hasMax) {
			packetByteBuf.writeLong(properties.max);
		}
	}

	public LongArgumentSerializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
		byte flags = packetByteBuf.readByte();
		long min = ArgumentHelper.hasMinFlag(flags) ? packetByteBuf.readLong() : Long.MIN_VALUE;
		long max = ArgumentHelper.hasMaxFlag(flags) ? packetByteBuf.readLong() : Long.MAX_VALUE;

		return new LongArgumentSerializer.Properties(min, max);
	}

	public void writeJson(LongArgumentSerializer.Properties properties, JsonObject jsonObject) {
		if (properties.min != Long.MIN_VALUE) {
			jsonObject.addProperty("min", properties.min);
		}

		if (properties.max != Long.MAX_VALUE) {
			jsonObject.addProperty("max", properties.max);
		}
	}

	public LongArgumentSerializer.Properties getArgumentTypeProperties(LongArgumentType longArgumentType) {
		return new LongArgumentSerializer.Properties(longArgumentType.getMinimum(), longArgumentType.getMaximum());
	}

	/**
	 * Свойства сериализатора: хранит диапазон допустимых значений.
	 */
	public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<LongArgumentType> {

		final long min;
		final long max;

		Properties(final long min, final long max) {
			this.min = min;
			this.max = max;
		}

		public LongArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
			return LongArgumentType.longArg(this.min, this.max);
		}

		@Override
		public ArgumentSerializer<LongArgumentType, ?> getSerializer() {
			return LongArgumentSerializer.this;
		}
	}
}
