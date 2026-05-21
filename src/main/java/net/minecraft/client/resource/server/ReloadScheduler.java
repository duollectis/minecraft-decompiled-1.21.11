package net.minecraft.client.resource.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
/**
 * {@code ReloadScheduler}.
 */
public interface ReloadScheduler {

	void scheduleReload(ReloadScheduler.ReloadContext context);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PackInfo}.
	 */
	public record PackInfo(UUID id, Path path) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ReloadContext}.
	 */
	public interface ReloadContext {

		void onSuccess();

		void onFailure(boolean force);

		List<ReloadScheduler.PackInfo> getPacks();
	}
}
