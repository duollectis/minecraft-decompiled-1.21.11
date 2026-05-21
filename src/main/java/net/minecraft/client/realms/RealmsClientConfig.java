package net.minecraft.client.realms;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.net.Proxy;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsClientConfig}.
 */
public class RealmsClientConfig {

	private static @Nullable Proxy proxy;

	public static @Nullable Proxy getProxy() {
		return proxy;
	}

	public static void setProxy(Proxy proxy) {
		if (RealmsClientConfig.proxy == null) {
			RealmsClientConfig.proxy = proxy;
		}
	}
}
