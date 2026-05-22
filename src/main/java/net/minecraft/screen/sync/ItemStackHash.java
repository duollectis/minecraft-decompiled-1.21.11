package net.minecraft.screen.sync;

import com.mojang.datafixers.DataFixUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

/**
 * Хэш-снимок предмета в слоте для эффективной сетевой синхронизации.
 * <p>
 * Позволяет клиенту проверить, изменился ли предмет в слоте, без полной
 * десериализации {@link ItemStack}. Если хэши совпадают — предмет считается
 * актуальным и полный пакет обновления не отправляется.
 */
public interface ItemStackHash {

	ItemStackHash EMPTY = new ItemStackHash() {
		@Override
		public String toString() {
			return "<empty>";
		}

		@Override
		public boolean hashEquals(ItemStack stack, ComponentChangesHash.ComponentHasher hasher) {
			return stack.isEmpty();
		}
	};

	PacketCodec<RegistryByteBuf, ItemStackHash> PACKET_CODEC = PacketCodecs
			.optional(Impl.PACKET_CODEC)
			.xmap(
					hash -> (ItemStackHash) DataFixUtils.orElse(hash, EMPTY),
					hash -> hash instanceof Impl impl ? Optional.of(impl) : Optional.empty()
			);

	boolean hashEquals(ItemStack stack, ComponentChangesHash.ComponentHasher hasher);

	/**
	 * Создаёт хэш-снимок из предмета. Для пустого предмета возвращает {@link #EMPTY}.
	 *
	 * @param stack  предмет для хэширования
	 * @param hasher функция вычисления хэша компонентов
	 * @return хэш-снимок предмета
	 */
	static ItemStackHash fromItemStack(ItemStack stack, ComponentChangesHash.ComponentHasher hasher) {
		return stack.isEmpty()
				? EMPTY
				: new Impl(
						stack.getRegistryEntry(),
						stack.getCount(),
						ComponentChangesHash.fromComponents(stack.getComponentChanges(), hasher)
				);
	}

	/**
	 * Конкретная реализация хэша непустого предмета.
	 *
	 * @param item       запись реестра типа предмета
	 * @param count      количество предметов в стаке
	 * @param components хэш изменений компонентов
	 */
	record Impl(RegistryEntry<Item> item, int count, ComponentChangesHash components) implements ItemStackHash {

		public static final PacketCodec<RegistryByteBuf, Impl> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.registryEntry(RegistryKeys.ITEM),
				Impl::item,
				PacketCodecs.VAR_INT,
				Impl::count,
				ComponentChangesHash.PACKET_CODEC,
				Impl::components,
				Impl::new
		);

		@Override
		public boolean hashEquals(ItemStack stack, ComponentChangesHash.ComponentHasher hasher) {
			if (count != stack.getCount()) {
				return false;
			}

			if (!item.equals(stack.getRegistryEntry())) {
				return false;
			}

			return components.hashEquals(stack.getComponentChanges(), hasher);
		}
	}
}
