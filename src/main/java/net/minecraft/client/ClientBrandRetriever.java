package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.obfuscate.DontObfuscate;

/**
 * Предоставляет идентификатор клиентской сборки.
 * Метод {@link #getClientModName()} намеренно не обфусцируется,
 * чтобы серверы могли определить тип клиента.
 */
@Environment(EnvType.CLIENT)
public class ClientBrandRetriever {

	public static final String VANILLA = "vanilla";

	@DontObfuscate
	public static String getClientModName() {
		return VANILLA;
	}
}
