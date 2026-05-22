package net.minecraft.screen.sync;

import net.minecraft.component.Component;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Хэш-снимок изменений компонентов предмета для эффективной сетевой синхронизации.
 * <p>
 * Вместо полной сериализации компонентов хранит хэши добавленных значений и набор
 * удалённых типов. Это позволяет клиенту быстро проверить, изменился ли предмет,
 * без полного декодирования данных компонентов.
 *
 * @param addedComponents   хэши добавленных/изменённых компонентов по типу
 * @param removedComponents типы удалённых компонентов
 */
public record ComponentChangesHash(
		Map<ComponentType<?>, Integer> addedComponents,
		Set<ComponentType<?>> removedComponents
) {

	public static final PacketCodec<RegistryByteBuf, ComponentChangesHash> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.map(
					HashMap::new,
					PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_TYPE),
					PacketCodecs.INTEGER,
					256
			),
			ComponentChangesHash::addedComponents,
			PacketCodecs.collection(HashSet::new, PacketCodecs.registryValue(RegistryKeys.DATA_COMPONENT_TYPE), 256),
			ComponentChangesHash::removedComponents,
			ComponentChangesHash::new
	);

	/**
	 * Создаёт хэш-снимок из полного набора изменений компонентов.
	 *
	 * @param changes изменения компонентов предмета
	 * @param hasher  функция вычисления хэша для отдельного компонента
	 * @return хэш-снимок изменений
	 */
	public static ComponentChangesHash fromComponents(
			ComponentChanges changes,
			ComponentHasher hasher
	) {
		ComponentChanges.AddedRemovedPair pair = changes.toAddedRemovedPair();
		Map<ComponentType<?>, Integer> hashes = new IdentityHashMap<>(pair.added().size());
		pair.added().forEach(component -> hashes.put(component.type(), hasher.apply((Component<?>) component)));
		return new ComponentChangesHash(hashes, pair.removed());
	}

	/**
	 * Проверяет, соответствует ли данный хэш-снимок фактическим изменениям компонентов.
	 * Используется на клиенте для определения необходимости полного обновления предмета.
	 *
	 * @param changes фактические изменения компонентов
	 * @param hasher  функция вычисления хэша
	 * @return {@code true} если хэши совпадают (предмет не изменился)
	 */
	public boolean hashEquals(ComponentChanges changes, ComponentHasher hasher) {
		ComponentChanges.AddedRemovedPair pair = changes.toAddedRemovedPair();

		if (!pair.removed().equals(removedComponents)) {
			return false;
		}

		if (addedComponents.size() != pair.added().size()) {
			return false;
		}

		for (Component<?> component : pair.added()) {
			Integer storedHash = addedComponents.get(component.type());

			if (storedHash == null) {
				return false;
			}

			if (!hasher.apply(component).equals(storedHash)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Функция вычисления целочисленного хэша для компонента предмета.
	 */
	@FunctionalInterface
	public interface ComponentHasher extends Function<Component<?>, Integer> {
	}
}
