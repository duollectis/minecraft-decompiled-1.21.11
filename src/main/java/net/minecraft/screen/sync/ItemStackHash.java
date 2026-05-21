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
 * {@code ItemStackHash}.
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

	PacketCodec<RegistryByteBuf, ItemStackHash> PACKET_CODEC = PacketCodecs.optional(ItemStackHash.Impl.PACKET_CODEC)
	                                                                       .xmap(
			                                                                       hash -> (ItemStackHash) DataFixUtils.orElse(
					                                                                       hash,
					                                                                       EMPTY
			                                                                       ),
			                                                                       hash -> hash instanceof ItemStackHash.Impl impl
			                                                                               ? Optional.of(impl)
			                                                                               : Optional.empty()
	                                                                       );

	boolean hashEquals(ItemStack stack, ComponentChangesHash.ComponentHasher hasher);

	static ItemStackHash fromItemStack(ItemStack stack, ComponentChangesHash.ComponentHasher hasher) {
		return (ItemStackHash) (stack.isEmpty()
		                        ? EMPTY
		                        : new ItemStackHash.Impl(
				                        stack.getRegistryEntry(),
				                        stack.getCount(),
				                        ComponentChangesHash.fromComponents(stack.getComponentChanges(), hasher)
		                        )
		);
	}

	/**
	 * {@code Impl}.
	 */
	public record Impl(RegistryEntry<Item> item, int count, ComponentChangesHash components) implements ItemStackHash {

		public static final PacketCodec<RegistryByteBuf, ItemStackHash.Impl> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.registryEntry(RegistryKeys.ITEM),
				ItemStackHash.Impl::item,
				PacketCodecs.VAR_INT,
				ItemStackHash.Impl::count,
				ComponentChangesHash.PACKET_CODEC,
				ItemStackHash.Impl::components,
				ItemStackHash.Impl::new
		);

		@Override
		public boolean hashEquals(ItemStack stack, ComponentChangesHash.ComponentHasher hasher) {
			if (this.count != stack.getCount()) {
				return false;
			}
			else {
				return !this.item.equals(stack.getRegistryEntry()) ? false : this.components.hashEquals(
						stack.getComponentChanges(),
						hasher
				);
			}
		}
	}
}
