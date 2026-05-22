package net.minecraft.stat;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Представляет одну игровую статистику, привязанную к конкретному значению реестра.
 *
 * <p>Каждый {@code Stat} является критерием скорборда и идентифицируется строкой вида
 * {@code "stat_type.namespace.path:value.namespace.path"}. Форматирование числового
 * значения делегируется {@link StatFormatter}.
 *
 * @param <T> тип значения, к которому привязана статистика (блок, предмет, сущность и т.д.)
 */
public class Stat<T> extends ScoreboardCriterion {

	/**
	 * Кодек для сериализации/десериализации {@code Stat} в сетевых пакетах.
	 * Диспетчеризация происходит по типу статистики ({@link StatType}).
	 */
	public static final PacketCodec<RegistryByteBuf, Stat<?>> PACKET_CODEC =
		PacketCodecs.registryValue(RegistryKeys.STAT_TYPE)
			.dispatch(Stat::getType, StatType::getPacketCodec);

	private final StatFormatter formatter;
	private final T value;
	private final StatType<T> type;

	protected Stat(StatType<T> type, T value, StatFormatter formatter) {
		super(buildName(type, value));
		this.type = type;
		this.formatter = formatter;
		this.value = value;
	}

	/**
	 * Строит строковый идентификатор статистики в формате
	 * {@code "statType.namespace.path:value.namespace.path"}.
	 *
	 * @param type  тип статистики
	 * @param value значение из реестра типа
	 * @param <T>   тип значения
	 * @return строковый идентификатор
	 */
	public static <T> String buildName(StatType<T> type, T value) {
		return identifierToName(Registries.STAT_TYPE.getId(type))
			+ ":"
			+ identifierToName(type.getRegistry().getId(value));
	}

	private static String identifierToName(@Nullable Identifier id) {
		return id.toString().replace(':', '.');
	}

	public StatType<T> getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	/**
	 * Форматирует числовое значение статистики в читаемую строку
	 * с помощью привязанного {@link StatFormatter}.
	 *
	 * @param rawValue сырое целочисленное значение статистики
	 * @return отформатированная строка (например, "1.23 km" или "5 min")
	 */
	public String format(int rawValue) {
		return formatter.format(rawValue);
	}

	@Override
	public boolean equals(Object other) {
		return this == other
			|| other instanceof Stat<?> stat && Objects.equals(getName(), stat.getName());
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String toString() {
		return "Stat{name=" + getName() + ", formatter=" + formatter + "}";
	}
}
