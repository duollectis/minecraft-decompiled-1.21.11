package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.function.Function;

/**
 * Класс registry byte buf.
 */
public class RegistryByteBuf extends PacketByteBuf {

	private final DynamicRegistryManager registryManager;

	public RegistryByteBuf(ByteBuf buf, DynamicRegistryManager registryManager) {
		super(buf);
		this.registryManager = registryManager;
	}

	public DynamicRegistryManager getRegistryManager() {
		return this.registryManager;
	}

	/**
	 * Make factory.
	 *
	 * @param registryManager registry manager
	 *
	 * @return Function — результат операции
	 */
	public static Function<ByteBuf, RegistryByteBuf> makeFactory(DynamicRegistryManager registryManager) {
		return buf -> new RegistryByteBuf(buf, registryManager);
	}
}
