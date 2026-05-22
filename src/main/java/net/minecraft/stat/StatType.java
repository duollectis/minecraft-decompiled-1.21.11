package net.minecraft.stat;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Тип статистики, связывающий реестр объектов (блоки, предметы, сущности и т.д.)
 * с набором конкретных {@link Stat} для каждого элемента этого реестра.
 *
 * <p>Экземпляры {@code StatType} регистрируются в {@code Registries.STAT_TYPE}.
 * Каждый тип хранит кэш уже созданных статистик в {@link IdentityHashMap},
 * что обеспечивает O(1) доступ по ссылке на объект реестра.
 *
 * @param <T> тип объектов реестра (например, {@code Block}, {@code Item})
 */
public class StatType<T> implements Iterable<Stat<T>> {

	private final Registry<T> registry;
	private final Map<T, Stat<T>> stats = new IdentityHashMap<>();
	private final Text name;
	private final PacketCodec<RegistryByteBuf, Stat<T>> packetCodec;

	public StatType(Registry<T> registry, Text name) {
		this.registry = registry;
		this.name = name;
		packetCodec = PacketCodecs.registryValue(registry.getKey())
			.xmap(this::getOrCreateStat, Stat::getValue);
	}

	public PacketCodec<RegistryByteBuf, Stat<T>> getPacketCodec() {
		return packetCodec;
	}

	public boolean hasStat(T key) {
		return stats.containsKey(key);
	}

	/**
	 * Возвращает существующую статистику для ключа или создаёт новую
	 * с указанным форматтером.
	 *
	 * @param key       ключ из реестра типа
	 * @param formatter форматтер для отображения значения
	 * @return статистика, связанная с ключом
	 */
	public Stat<T> getOrCreateStat(T key, StatFormatter formatter) {
		return stats.computeIfAbsent(key, k -> new Stat<>(this, k, formatter));
	}

	/**
	 * Возвращает существующую статистику для ключа или создаёт новую
	 * с форматтером по умолчанию ({@link StatFormatter#DEFAULT}).
	 *
	 * @param key ключ из реестра типа
	 * @return статистика, связанная с ключом
	 */
	public Stat<T> getOrCreateStat(T key) {
		return getOrCreateStat(key, StatFormatter.DEFAULT);
	}

	public Registry<T> getRegistry() {
		return registry;
	}

	public Text getName() {
		return name;
	}

	@Override
	public Iterator<Stat<T>> iterator() {
		return stats.values().iterator();
	}
}
