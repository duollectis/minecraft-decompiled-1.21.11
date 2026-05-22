package net.minecraft.client.resource.server;

import com.google.common.hash.HashCode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Downloader;
import org.jspecify.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

/**
 * Менеджер серверных ресурс-паков на стороне клиента.
 * Управляет полным жизненным циклом пака: добавление → принятие/отклонение →
 * скачивание → применение через {@link ReloadScheduler} → удаление.
 *
 * <p>Статус принятия ({@link AcceptanceStatus}) определяет, будут ли новые паки
 * автоматически приняты или отклонены. Метод {@link #update()} вызывается
 * периодически для продвижения очереди загрузок и применения готовых паков.
 */
@Environment(EnvType.CLIENT)
public class ServerResourcePackManager {

	private final DownloadQueuer queuer;
	final PackStateChangeCallback stateChangeCallback;
	private final ReloadScheduler reloadScheduler;
	private final Runnable packChangeCallback;
	private ServerResourcePackManager.AcceptanceStatus acceptanceStatus;
	final List<ServerResourcePackManager.PackEntry> packs = new ArrayList<>();

	public ServerResourcePackManager(
			DownloadQueuer queuer,
			PackStateChangeCallback stateChangeCallback,
			ReloadScheduler reloadScheduler,
			Runnable packChangeCallback,
			ServerResourcePackManager.AcceptanceStatus acceptanceStatus
	) {
		this.queuer = queuer;
		this.stateChangeCallback = stateChangeCallback;
		this.reloadScheduler = reloadScheduler;
		this.packChangeCallback = packChangeCallback;
		this.acceptanceStatus = acceptanceStatus;
	}

	void onPackChanged() {
		packChangeCallback.run();
	}

	private void markReplaced(UUID id) {
		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (packEntry.id.equals(id)) {
				packEntry.discard(ServerResourcePackManager.DiscardReason.SERVER_REPLACED);
			}
		}
	}

	/**
	 * Добавляет серверный пак для скачивания по URL.
	 * Если статус принятия — {@link AcceptanceStatus#DECLINED}, пак немедленно отклоняется.
	 *
	 * @param id       уникальный идентификатор пака
	 * @param url      URL для скачивания
	 * @param hashCode ожидаемый хэш файла для верификации, или {@code null}
	 */
	public void addResourcePack(UUID id, URL url, @Nullable HashCode hashCode) {
		if (acceptanceStatus == ServerResourcePackManager.AcceptanceStatus.DECLINED) {
			stateChangeCallback.onFinish(id, PackStateChangeCallback.FinishState.DECLINED);
			return;
		}

		onAdd(id, new ServerResourcePackManager.PackEntry(id, url, hashCode));
	}

	/**
	 * Добавляет серверный пак из локального файла (уже скачан).
	 * Конвертирует {@link Path} в {@link URL} и помечает пак как {@link LoadStatus#DONE}.
	 *
	 * @param id   уникальный идентификатор пака
	 * @param path путь к локальному файлу пака
	 */
	public void addResourcePack(UUID id, Path path) {
		if (acceptanceStatus == ServerResourcePackManager.AcceptanceStatus.DECLINED) {
			stateChangeCallback.onFinish(id, PackStateChangeCallback.FinishState.DECLINED);
			return;
		}

		URL url;
		try {
			url = path.toUri().toURL();
		}
		catch (MalformedURLException exception) {
			throw new IllegalStateException("Can't convert path to URL " + path, exception);
		}

		ServerResourcePackManager.PackEntry packEntry = new ServerResourcePackManager.PackEntry(id, url, null);
		packEntry.loadStatus = ServerResourcePackManager.LoadStatus.DONE;
		packEntry.path = path;
		onAdd(id, packEntry);
	}

	private void onAdd(UUID id, ServerResourcePackManager.PackEntry pack) {
		markReplaced(id);
		packs.add(pack);
		if (acceptanceStatus == ServerResourcePackManager.AcceptanceStatus.ALLOWED) {
			accept(pack);
		}

		onPackChanged();
	}

	private void accept(ServerResourcePackManager.PackEntry pack) {
		stateChangeCallback.onStateChanged(pack.id, PackStateChangeCallback.State.ACCEPTED);
		pack.accepted = true;
	}

	private ServerResourcePackManager.@Nullable PackEntry get(UUID id) {
		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (!packEntry.isDiscarded() && packEntry.id.equals(id)) {
				return packEntry;
			}
		}

		return null;
	}

	public void remove(UUID id) {
		ServerResourcePackManager.PackEntry packEntry = get(id);
		if (packEntry != null) {
			packEntry.discard(ServerResourcePackManager.DiscardReason.SERVER_REMOVED);
			onPackChanged();
		}
	}

	public void removeAll() {
		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			packEntry.discard(ServerResourcePackManager.DiscardReason.SERVER_REMOVED);
		}

		onPackChanged();
	}

	public void acceptAll() {
		acceptanceStatus = ServerResourcePackManager.AcceptanceStatus.ALLOWED;

		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (!packEntry.accepted && !packEntry.isDiscarded()) {
				accept(packEntry);
			}
		}

		onPackChanged();
	}

	public void declineAll() {
		acceptanceStatus = ServerResourcePackManager.AcceptanceStatus.DECLINED;

		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (!packEntry.accepted) {
				packEntry.discard(ServerResourcePackManager.DiscardReason.DECLINED);
			}
		}

		onPackChanged();
	}

	public void resetAcceptanceStatus() {
		acceptanceStatus = ServerResourcePackManager.AcceptanceStatus.PENDING;
	}

	public void update() {
		boolean hasActiveDownloads = enqueueDownloads();
		if (!hasActiveDownloads) {
			applyDownloadedPacks();
		}

		removeInactivePacks();
	}

	private void removeInactivePacks() {
		packs.removeIf(pack -> {
			if (pack.status != ServerResourcePackManager.Status.INACTIVE) {
				return false;
			}

			if (pack.discardReason == null) {
				return false;
			}

			PackStateChangeCallback.FinishState finishState = pack.discardReason.state;
			if (finishState != null) {
				stateChangeCallback.onFinish(pack.id, finishState);
			}

			return true;
		});
	}

	private void onDownload(Collection<ServerResourcePackManager.PackEntry> packs, Downloader.DownloadResult result) {
		if (!result.failed().isEmpty()) {
			for (ServerResourcePackManager.PackEntry packEntry : this.packs) {
				if (packEntry.status != ServerResourcePackManager.Status.ACTIVE) {
					if (result.failed().contains(packEntry.id)) {
						packEntry.discard(ServerResourcePackManager.DiscardReason.DOWNLOAD_FAILED);
					}
					else {
						packEntry.discard(ServerResourcePackManager.DiscardReason.DISCARDED);
					}
				}
			}
		}

		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			Path path = result.downloaded().get(packEntry.id);
			if (path != null) {
				packEntry.loadStatus = ServerResourcePackManager.LoadStatus.DONE;
				packEntry.path = path;
				if (!packEntry.isDiscarded()) {
					stateChangeCallback.onStateChanged(packEntry.id, PackStateChangeCallback.State.DOWNLOADED);
				}
			}
		}

		onPackChanged();
	}

	private boolean enqueueDownloads() {
		List<ServerResourcePackManager.PackEntry> toDownload = new ArrayList<>();
		boolean hasActiveDownloads = false;

		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (!packEntry.isDiscarded() && packEntry.accepted) {
				if (packEntry.loadStatus != ServerResourcePackManager.LoadStatus.DONE) {
					hasActiveDownloads = true;
				}

				if (packEntry.loadStatus == ServerResourcePackManager.LoadStatus.REQUESTED) {
					packEntry.loadStatus = ServerResourcePackManager.LoadStatus.PENDING;
					toDownload.add(packEntry);
				}
			}
		}

		if (!toDownload.isEmpty()) {
			Map<UUID, Downloader.DownloadEntry> downloadMap = new HashMap<>();

			for (ServerResourcePackManager.PackEntry packEntry : toDownload) {
				downloadMap.put(packEntry.id, new Downloader.DownloadEntry(packEntry.url, packEntry.hashCode));
			}

			queuer.enqueue(downloadMap, result -> onDownload(toDownload, result));
		}

		return hasActiveDownloads;
	}

	/**
	 * Применяет все скачанные и принятые паки через {@link ReloadScheduler}.
	 * Строит два списка: паки для активации ({@code toActivate}) и паки для деактивации
	 * ({@code toDeactivate}). Если есть изменения — переводит их в статус PENDING
	 * и запускает перезагрузку ресурсов.
	 */
	private void applyDownloadedPacks() {
		boolean hasChanges = false;
		final List<ServerResourcePackManager.PackEntry> toActivate = new ArrayList<>();
		final List<ServerResourcePackManager.PackEntry> toDeactivate = new ArrayList<>();

		for (ServerResourcePackManager.PackEntry packEntry : packs) {
			if (packEntry.status == ServerResourcePackManager.Status.PENDING) {
				return;
			}

			boolean isReady = packEntry.accepted
					&& packEntry.loadStatus == ServerResourcePackManager.LoadStatus.DONE
					&& !packEntry.isDiscarded();

			if (isReady && packEntry.status == ServerResourcePackManager.Status.INACTIVE) {
				toActivate.add(packEntry);
				hasChanges = true;
			}

			if (packEntry.status == ServerResourcePackManager.Status.ACTIVE) {
				if (!isReady) {
					hasChanges = true;
					toDeactivate.add(packEntry);
				}
				else {
					toActivate.add(packEntry);
				}
			}
		}

		if (!hasChanges) {
			return;
		}

		for (ServerResourcePackManager.PackEntry packEntry : toActivate) {
			if (packEntry.status != ServerResourcePackManager.Status.ACTIVE) {
				packEntry.status = ServerResourcePackManager.Status.PENDING;
			}
		}

		for (ServerResourcePackManager.PackEntry packEntry : toDeactivate) {
			packEntry.status = ServerResourcePackManager.Status.PENDING;
		}

		reloadScheduler.scheduleReload(new ReloadScheduler.ReloadContext() {
			@Override
			public void onSuccess() {
				for (ServerResourcePackManager.PackEntry packEntry : toActivate) {
					packEntry.status = ServerResourcePackManager.Status.ACTIVE;
					if (packEntry.discardReason == null) {
						ServerResourcePackManager.this.stateChangeCallback.onFinish(
								packEntry.id,
								PackStateChangeCallback.FinishState.APPLIED
						);
					}
				}

				for (ServerResourcePackManager.PackEntry packEntry : toDeactivate) {
					packEntry.status = ServerResourcePackManager.Status.INACTIVE;
				}

				ServerResourcePackManager.this.onPackChanged();
			}

			@Override
			public void onFailure(boolean force) {
				if (force) {
					for (ServerResourcePackManager.PackEntry packEntry : ServerResourcePackManager.this.packs) {
						if (packEntry.status == ServerResourcePackManager.Status.PENDING) {
							packEntry.status = ServerResourcePackManager.Status.INACTIVE;
						}
					}
					return;
				}

				toActivate.clear();

				for (ServerResourcePackManager.PackEntry packEntry : ServerResourcePackManager.this.packs) {
					switch (packEntry.status) {
						case INACTIVE:
							packEntry.discard(ServerResourcePackManager.DiscardReason.DISCARDED);
							break;
						case PENDING:
							packEntry.status = ServerResourcePackManager.Status.INACTIVE;
							packEntry.discard(ServerResourcePackManager.DiscardReason.ACTIVATION_FAILED);
							break;
						case ACTIVE:
							toActivate.add(packEntry);
					}
				}

				ServerResourcePackManager.this.onPackChanged();
			}

			@Override
			public List<ReloadScheduler.PackInfo> getPacks() {
				return toActivate.stream()
						.map(pack -> new ReloadScheduler.PackInfo(pack.id, pack.path))
						.toList();
			}
		});
	}

	/** Статус принятия серверных ресурс-паков пользователем. */
	@Environment(EnvType.CLIENT)
	public static enum AcceptanceStatus {
		PENDING,
		ALLOWED,
		DECLINED;
	}

	/** Причина отбрасывания пака с соответствующим финальным состоянием колбэка. */
	@Environment(EnvType.CLIENT)
	static enum DiscardReason {
		DOWNLOAD_FAILED(PackStateChangeCallback.FinishState.DOWNLOAD_FAILED),
		ACTIVATION_FAILED(PackStateChangeCallback.FinishState.ACTIVATION_FAILED),
		DECLINED(PackStateChangeCallback.FinishState.DECLINED),
		DISCARDED(PackStateChangeCallback.FinishState.DISCARDED),
		SERVER_REMOVED(null),
		SERVER_REPLACED(null);

		final PackStateChangeCallback.@Nullable FinishState state;

		private DiscardReason(final PackStateChangeCallback.FinishState state) {
			this.state = state;
		}
	}

	/** Статус загрузки файла пака с сервера. */
	@Environment(EnvType.CLIENT)
	static enum LoadStatus {
		REQUESTED,
		PENDING,
		DONE;
	}

	/** Запись об одном серверном ресурс-паке с его метаданными и текущим состоянием. */
	@Environment(EnvType.CLIENT)
	static class PackEntry {

		final UUID id;
		final URL url;
		final @Nullable HashCode hashCode;
		@Nullable Path path;
		ServerResourcePackManager.@Nullable DiscardReason discardReason;
		ServerResourcePackManager.LoadStatus loadStatus = ServerResourcePackManager.LoadStatus.REQUESTED;
		ServerResourcePackManager.Status status = ServerResourcePackManager.Status.INACTIVE;
		boolean accepted;

		PackEntry(UUID id, URL url, @Nullable HashCode hashCode) {
			this.id = id;
			this.url = url;
			this.hashCode = hashCode;
		}

		public void discard(ServerResourcePackManager.DiscardReason reason) {
			if (discardReason == null) {
				discardReason = reason;
			}
		}

		public boolean isDiscarded() {
			return discardReason != null;
		}
	}

	/** Статус активности пака в системе ресурсов клиента. */
	@Environment(EnvType.CLIENT)
	static enum Status {
		INACTIVE,
		PENDING,
		ACTIVE;
	}
}
