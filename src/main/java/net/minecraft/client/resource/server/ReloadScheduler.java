package net.minecraft.client.resource.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Планировщик перезагрузки ресурсов при смене набора активных серверных паков.
 * Реализация должна запустить перезагрузку и уведомить {@link ReloadContext}
 * об успехе или неудаче через {@link ReloadContext#onSuccess()} / {@link ReloadContext#onFailure}.
 */
@Environment(EnvType.CLIENT)
public interface ReloadScheduler {

	void scheduleReload(ReloadScheduler.ReloadContext context);

	/** Информация об одном серверном паке, участвующем в перезагрузке. */
	@Environment(EnvType.CLIENT)
	record PackInfo(UUID id, Path path) {
	}

	/** Контекст перезагрузки: предоставляет список паков и колбэки результата. */
	@Environment(EnvType.CLIENT)
	interface ReloadContext {

		void onSuccess();

		void onFailure(boolean force);

		List<ReloadScheduler.PackInfo> getPacks();
	}
}
