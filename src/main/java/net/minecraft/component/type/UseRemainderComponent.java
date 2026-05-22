package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
	 * Компонент остатка после использования предмета. Определяет, во что превращается
	 * предмет после полного использования (например, стеклянная бутылка после зелья).
	 */
public record UseRemainderComponent(ItemStack convertInto) {

	public static final Codec<UseRemainderComponent>
			CODEC =
			ItemStack.CODEC.xmap(UseRemainderComponent::new, UseRemainderComponent::convertInto);
	public static final PacketCodec<RegistryByteBuf, UseRemainderComponent> PACKET_CODEC = PacketCodec.tuple(
			ItemStack.PACKET_CODEC, UseRemainderComponent::convertInto, UseRemainderComponent::new
	);

	/**
		 * Конвертирует стек предмета в остаток после использования.
		 * В режиме творчества стек не изменяется. Если количество предметов не уменьшилось —
		 * конвертация не нужна. Если стек опустел — возвращает копию остатка напрямую,
		 * иначе передаёт остаток через {@code inserter} (например, в инвентарь).
		 *
		 * @param stack      текущий стек предмета после использования
		 * @param oldCount   количество предметов до использования
		 * @param inCreative {@code true} если пользователь в режиме творчества
		 * @param inserter   функция для вставки остатка (например, в инвентарь игрока)
		 * @return итоговый стек предмета
		 */
	public ItemStack convert(
			ItemStack stack,
			int oldCount,
			boolean inCreative,
			UseRemainderComponent.StackInserter inserter
	) {
		if (inCreative) {
			return stack;
		}

		if (stack.getCount() >= oldCount) {
			return stack;
		}

		ItemStack remainder = convertInto.copy();

		if (stack.isEmpty()) {
			return remainder;
		}

		inserter.apply(remainder);
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UseRemainderComponent other = (UseRemainderComponent) o;
		return ItemStack.areEqual(convertInto, other.convertInto);
	}

	@Override
	public int hashCode() {
		return ItemStack.hashCode(convertInto);
	}

	/**
		 * Функциональный интерфейс для вставки стека остатка в инвентарь или мир.
		 */
	@FunctionalInterface
	public interface StackInserter {

		void apply(ItemStack stack);
	}
}
