package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.function.Function;

public class RegistryByteBuf extends PacketByteBuf {

	private final DynamicRegistryManager registryManager;

	public RegistryByteBuf(ByteBuf buf, DynamicRegistryManager registryManager) {
		super(buf);
		this.registryManager = registryManager;
	}

	public DynamicRegistryManager getRegistryManager() {
		return this.registryManager;
	}

	public static Function<ByteBuf, RegistryByteBuf> makeFactory(DynamicRegistryManager registryManager) {
		return buf -> new RegistryByteBuf(buf, registryManager);
	}
}
