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
 * {@code NbtComponent}.
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
	 * Of.
	 *
	 * @param nbt nbt
	 *
	 * @return NbtComponent — результат операции
	 */
	public static NbtComponent of(NbtCompound nbt) {
		return new NbtComponent(nbt.copy());
	}

	/**
	 * Matches.
	 *
	 * @param nbt nbt
	 *
	 * @return boolean — результат операции
	 */
	public boolean matches(NbtCompound nbt) {
		return NbtHelper.matches(nbt, this.nbt, true);
	}

	/**
	 * Set.
	 *
	 * @param type type
	 * @param stack stack
	 * @param nbtSetter nbt setter
	 */
	public static void set(ComponentType<NbtComponent> type, ItemStack stack, Consumer<NbtCompound> nbtSetter) {
		NbtComponent nbtComponent = stack.getOrDefault(type, DEFAULT).apply(nbtSetter);
		if (nbtComponent.nbt.isEmpty()) {
			stack.remove(type);
		}
		else {
			stack.set(type, nbtComponent);
		}
	}

	/**
	 * Set.
	 *
	 * @param type type
	 * @param stack stack
	 * @param nbt nbt
	 */
	public static void set(ComponentType<NbtComponent> type, ItemStack stack, NbtCompound nbt) {
		if (!nbt.isEmpty()) {
			stack.set(type, of(nbt));
		}
		else {
			stack.remove(type);
		}
	}

	/**
	 * Apply.
	 *
	 * @param nbtConsumer nbt consumer
	 *
	 * @return NbtComponent — результат операции
	 */
	public NbtComponent apply(Consumer<NbtCompound> nbtConsumer) {
		NbtCompound nbtCompound = this.nbt.copy();
		nbtConsumer.accept(nbtCompound);
		return new NbtComponent(nbtCompound);
	}

	public boolean isEmpty() {
		return this.nbt.isEmpty();
	}

	/**
	 * Создаёт копию nbt.
	 *
	 * @return NbtCompound — результат операции
	 */
	public NbtCompound copyNbt() {
		return this.nbt.copy();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else {
			return o instanceof NbtComponent nbtComponent ? this.nbt.equals(nbtComponent.nbt) : false;
		}
	}

	@Override
	public int hashCode() {
		return this.nbt.hashCode();
	}

	@Override
	public String toString() {
		return this.nbt.toString();
	}
}
