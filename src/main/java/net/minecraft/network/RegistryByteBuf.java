package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.function.Function;

/**
 * Расширение {@link PacketByteBuf} с доступом к {@link DynamicRegistryManager}.
 * <p>Используется в пакетах, которым для кодирования/декодирования необходим контекст
 * динамических реестров (например, предметы, биомы, измерения).
 */
public class RegistryByteBuf extends PacketByteBuf {

	private final DynamicRegistryManager registryManager;

	public RegistryByteBuf(ByteBuf buf, DynamicRegistryManager registryManager) {
		super(buf);
		this.registryManager = registryManager;
	}

	public DynamicRegistryManager getRegistryManager() {
		return registryManager;
	}

	/**
	 * Создаёт фабрику {@link RegistryByteBuf} с привязанным менеджером реестров.
	 * Используется как {@code Function<ByteBuf, RegistryByteBuf>} при создании кодеков.
	 *
	 * @param registryManager менеджер динамических реестров
	 * @return функция-фабрика, оборачивающая {@link ByteBuf} в {@link RegistryByteBuf}
	 */
	public static Function<ByteBuf, RegistryByteBuf> makeFactory(DynamicRegistryManager registryManager) {
		return buf -> new RegistryByteBuf(buf, registryManager);
	}
}
