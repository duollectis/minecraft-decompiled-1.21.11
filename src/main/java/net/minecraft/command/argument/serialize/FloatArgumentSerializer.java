package net.minecraft.command.argument.serialize;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.network.PacketByteBuf;

/**
 * Сериализатор аргумента {@link FloatArgumentType} для передачи по сети и записи в JSON.
 * Передаёт минимальное и максимальное допустимые значения через битовый флаг присутствия.
 */
public class FloatArgumentSerializer implements ArgumentSerializer<FloatArgumentType, FloatArgumentSerializer.Properties> {

	public void writePacket(FloatArgumentSerializer.Properties properties, PacketByteBuf packetByteBuf) {
		boolean hasMin = properties.min != -Float.MAX_VALUE;
		boolean hasMax = properties.max != Float.MAX_VALUE;

		packetByteBuf.writeByte(ArgumentHelper.getMinMaxFlag(hasMin, hasMax));

		if (hasMin) {
			packetByteBuf.writeFloat(properties.min);
		}

		if (hasMax) {
			packetByteBuf.writeFloat(properties.max);
		}
	}

	public FloatArgumentSerializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
		byte flags = packetByteBuf.readByte();
		float min = ArgumentHelper.hasMinFlag(flags) ? packetByteBuf.readFloat() : -Float.MAX_VALUE;
		float max = ArgumentHelper.hasMaxFlag(flags) ? packetByteBuf.readFloat() : Float.MAX_VALUE;

		return new FloatArgumentSerializer.Properties(min, max);
	}

	public void writeJson(FloatArgumentSerializer.Properties properties, JsonObject jsonObject) {
		if (properties.min != -Float.MAX_VALUE) {
			jsonObject.addProperty("min", properties.min);
		}

		if (properties.max != Float.MAX_VALUE) {
			jsonObject.addProperty("max", properties.max);
		}
	}

	public FloatArgumentSerializer.Properties getArgumentTypeProperties(FloatArgumentType floatArgumentType) {
		return new FloatArgumentSerializer.Properties(floatArgumentType.getMinimum(), floatArgumentType.getMaximum());
	}

	/**
	 * Свойства сериализатора: хранит диапазон допустимых значений.
	 */
	public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<FloatArgumentType> {

		final float min;
		final float max;

		Properties(final float min, final float max) {
			this.min = min;
			this.max = max;
		}

		public FloatArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
			return FloatArgumentType.floatArg(this.min, this.max);
		}

		@Override
		public ArgumentSerializer<FloatArgumentType, ?> getSerializer() {
			return FloatArgumentSerializer.this;
		}
	}
}
