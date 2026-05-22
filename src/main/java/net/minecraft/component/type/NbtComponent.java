package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.function.Consumer;

/**
	 * Компонент произвольных NBT-данных предмета. Используется для хранения
	 * кастомных данных, не покрытых стандартными компонентами (например, данные модов).
	 */
public final class NbtComponent {

	public static final NbtComponent DEFAULT = new NbtComponent(new NbtCompound());
	public static final Codec<NbtCompound>
			COMPOUND_CODEC =
			Codec.withAlternative(NbtCompound.CODEC, StringNbtReader.STRINGIFIED_CODEC);
	public static final Codec<NbtComponent> CODEC = COMPOUND_CODEC.xmap(NbtComponent::new, component -> component.nbt);
	@Deprecated
	public static final PacketCodec<ByteBuf, NbtComponent>
			PACKET_CODEC =
			PacketCodecs.NBT_COMPOUND.xmap(NbtComponent::new, component -> component.nbt);
	private final NbtCompound nbt;

	private NbtComponent(NbtCompound nbt) {
		this.nbt = nbt;
	}

	/**
		 * Создаёт компонент с защитной копией переданного NBT-тега.
		 *
		 * @param nbt исходный NBT-тег (будет скопирован)
		 * @return новый {@code NbtComponent}
		 */
	public static NbtComponent of(NbtCompound nbt) {
		return new NbtComponent(nbt.copy());
	}

	/**
		 * Проверяет, содержит ли компонент все ключи и значения из переданного NBT-тега
		 * (частичное совпадение — дополнительные ключи в компоненте допустимы).
		 *
		 * @param nbt NBT-тег для сравнения
		 * @return {@code true} если все поля {@code nbt} присутствуют в компоненте
		 */
	public boolean matches(NbtCompound nbt) {
		return NbtHelper.matches(nbt, this.nbt, true);
	}

	/**
		 * Изменяет NBT-компонент стека через функцию-мутатор. Если после изменения
		 * NBT оказывается пустым — компонент удаляется из стека.
		 *
		 * @param type      тип компонента
		 * @param stack     целевой стек предмета
		 * @param nbtSetter функция, изменяющая NBT-тег
		 */
	public static void set(ComponentType<NbtComponent> type, ItemStack stack, Consumer<NbtCompound> nbtSetter) {
		NbtComponent updated = stack.getOrDefault(type, DEFAULT).apply(nbtSetter);

		if (updated.nbt.isEmpty()) {
			stack.remove(type);
		} else {
			stack.set(type, updated);
		}
	}

	/**
		 * Устанавливает NBT-компонент стека напрямую. Если NBT пустой — компонент удаляется.
		 *
		 * @param type  тип компонента
		 * @param stack целевой стек предмета
		 * @param nbt   новый NBT-тег
		 */
	public static void set(ComponentType<NbtComponent> type, ItemStack stack, NbtCompound nbt) {
		if (nbt.isEmpty()) {
			stack.remove(type);
		} else {
			stack.set(type, of(nbt));
		}
	}

	/**
		 * Возвращает новый компонент с изменённым NBT-тегом через функцию-мутатор.
		 *
		 * @param nbtConsumer функция, изменяющая копию внутреннего NBT-тега
		 * @return новый {@code NbtComponent} с изменёнными данными
		 */
	public NbtComponent apply(Consumer<NbtCompound> nbtConsumer) {
		NbtCompound copy = nbt.copy();
		nbtConsumer.accept(copy);
		return new NbtComponent(copy);
	}

	public boolean isEmpty() {
		return nbt.isEmpty();
	}

	public NbtCompound copyNbt() {
		return nbt.copy();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		return o instanceof NbtComponent other && nbt.equals(other.nbt);
	}

	@Override
	public int hashCode() {
		return nbt.hashCode();
	}

	@Override
	public String toString() {
		return nbt.toString();
	}
}
