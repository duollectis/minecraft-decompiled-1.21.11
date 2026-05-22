package net.minecraft.client.resource.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

/**
 * Колбэк для отслеживания изменений состояния серверного ресурс-пака.
 * Вызывается при промежуточных переходах ({@link #onStateChanged}) и
 * при финальном завершении обработки пака ({@link #onFinish}).
 */
@Environment(EnvType.CLIENT)
public interface PackStateChangeCallback {

	void onStateChanged(UUID id, PackStateChangeCallback.State state);

	void onFinish(UUID id, PackStateChangeCallback.FinishState state);

	/** Финальный результат обработки серверного ресурс-пака. */
	@Environment(EnvType.CLIENT)
	enum FinishState {
		DECLINED,
		APPLIED,
		DISCARDED,
		DOWNLOAD_FAILED,
		ACTIVATION_FAILED;
	}

	/** Промежуточное состояние принятого серверного ресурс-пака. */
	@Environment(EnvType.CLIENT)
	enum State {
		ACCEPTED,
		DOWNLOADED;
	}
}
