package net.minecraft.command.argument.serialize;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.network.PacketByteBuf;

/**
 * Сериализатор аргумента {@link DoubleArgumentType} для передачи по сети и записи в JSON.
 * Передаёт минимальное и максимальное допустимые значения через битовый флаг присутствия.
 */
public class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType, DoubleArgumentSerializer.Properties> {

	public void writePacket(DoubleArgumentSerializer.Properties properties, PacketByteBuf packetByteBuf) {
		boolean hasMin = properties.min != -Double.MAX_VALUE;
		boolean hasMax = properties.max != Double.MAX_VALUE;

		packetByteBuf.writeByte(ArgumentHelper.getMinMaxFlag(hasMin, hasMax));

		if (hasMin) {
			packetByteBuf.writeDouble(properties.min);
		}

		if (hasMax) {
			packetByteBuf.writeDouble(properties.max);
		}
	}

	public DoubleArgumentSerializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
		byte flags = packetByteBuf.readByte();
		double min = ArgumentHelper.hasMinFlag(flags) ? packetByteBuf.readDouble() : -Double.MAX_VALUE;
		double max = ArgumentHelper.hasMaxFlag(flags) ? packetByteBuf.readDouble() : Double.MAX_VALUE;

		return new DoubleArgumentSerializer.Properties(min, max);
	}

	public void writeJson(DoubleArgumentSerializer.Properties properties, JsonObject jsonObject) {
		if (properties.min != -Double.MAX_VALUE) {
			jsonObject.addProperty("min", properties.min);
		}

		if (properties.max != Double.MAX_VALUE) {
			jsonObject.addProperty("max", properties.max);
		}
	}

	public DoubleArgumentSerializer.Properties getArgumentTypeProperties(DoubleArgumentType doubleArgumentType) {
		return new DoubleArgumentSerializer.Properties(
				doubleArgumentType.getMinimum(),
				doubleArgumentType.getMaximum()
		);
	}

	/**
	 * Свойства сериализатора: хранит диапазон допустимых значений.
	 */
	public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<DoubleArgumentType> {

		final double min;
		final double max;

		Properties(final double min, final double max) {
			this.min = min;
			this.max = max;
		}

		public DoubleArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
			return DoubleArgumentType.doubleArg(this.min, this.max);
		}

		@Override
		public ArgumentSerializer<DoubleArgumentType, ?> getSerializer() {
			return DoubleArgumentSerializer.this;
		}
	}
}
