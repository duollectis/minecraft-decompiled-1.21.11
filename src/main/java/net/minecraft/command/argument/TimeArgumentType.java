package net.minecraft.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Тип аргумента команды Brigadier для ввода временных интервалов в игровых тиках.
 * <p>
 * Поддерживает суффиксы единиц времени:
 * <ul>
 *   <li>{@code d} — игровые дни (1d = 24000 тиков)</li>
 *   <li>{@code s} — секунды (1s = 20 тиков)</li>
 *   <li>{@code t} или без суффикса — тики (1:1)</li>
 * </ul>
 * Результат всегда возвращается в тиках. Поддерживает минимальное значение.
 */
public class TimeArgumentType implements ArgumentType<Integer> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
	private static final SimpleCommandExceptionType
			INVALID_UNIT_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.time.invalid_unit"));
	private static final Dynamic2CommandExceptionType TICK_COUNT_TOO_LOW_EXCEPTION = new Dynamic2CommandExceptionType(
			(value, minimum) -> Text.stringifiedTranslatable("argument.time.tick_count_too_low", minimum, value)
	);
	private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap();
	final int minimum;

	private TimeArgumentType(int minimum) {
		this.minimum = minimum;
	}

	public static TimeArgumentType time() {
		return new TimeArgumentType(0);
	}

	public static TimeArgumentType time(int minimum) {
		return new TimeArgumentType(minimum);
	}

	public Integer parse(StringReader stringReader) throws CommandSyntaxException {
		float amount = stringReader.readFloat();
		String unit = stringReader.readUnquotedString();
		int ticksPerUnit = UNITS.getOrDefault(unit, 0);

		if (ticksPerUnit == 0) {
			throw INVALID_UNIT_EXCEPTION.createWithContext(stringReader);
		}

		int totalTicks = Math.round(amount * ticksPerUnit);

		if (totalTicks < minimum) {
			throw TICK_COUNT_TOO_LOW_EXCEPTION.createWithContext(stringReader, totalTicks, minimum);
		}

		return totalTicks;
	}

	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		StringReader remaining = new StringReader(builder.getRemaining());

		try {
			remaining.readFloat();
		}
		catch (CommandSyntaxException ignored) {
			return builder.buildFuture();
		}

		return CommandSource.suggestMatching(
				UNITS.keySet(),
				builder.createOffset(builder.getStart() + remaining.getCursor())
		);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	static {
		UNITS.put("d", 24000);
		UNITS.put("s", 20);
		UNITS.put("t", 1);
		UNITS.put("", 1);
	}

	/**
	 * Сериализатор аргумента для передачи по сети и записи в JSON.
	 * Передаёт минимально допустимое значение в тиках.
	 */
	public static class Serializer implements ArgumentSerializer<TimeArgumentType, TimeArgumentType.Serializer.Properties> {

		public void writePacket(TimeArgumentType.Serializer.Properties properties, PacketByteBuf packetByteBuf) {
			packetByteBuf.writeInt(properties.minimum);
		}

		public TimeArgumentType.Serializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
			int minimum = packetByteBuf.readInt();

			return new TimeArgumentType.Serializer.Properties(minimum);
		}

		public void writeJson(TimeArgumentType.Serializer.Properties properties, JsonObject jsonObject) {
			jsonObject.addProperty("min", properties.minimum);
		}

		public TimeArgumentType.Serializer.Properties getArgumentTypeProperties(TimeArgumentType timeArgumentType) {
			return new TimeArgumentType.Serializer.Properties(timeArgumentType.minimum);
		}

		/**
		 * Свойства сериализатора: хранит минимально допустимое значение в тиках.
		 */
		public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<TimeArgumentType> {

			final int minimum;

			Properties(final int minimum) {
				this.minimum = minimum;
			}

			public TimeArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
				return TimeArgumentType.time(this.minimum);
			}

			@Override
			public ArgumentSerializer<TimeArgumentType, ?> getSerializer() {
				return Serializer.this;
			}
		}
	}
}
