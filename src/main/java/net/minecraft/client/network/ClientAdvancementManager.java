package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.network.packet.c2s.play.AdvancementTabC2SPacket;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Клиентский менеджер достижений.
 * Синхронизирует состояние достижений с сервером, показывает тосты
 * и уведомляет слушателя об изменениях прогресса.
 */
@Environment(EnvType.CLIENT)
public class ClientAdvancementManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final MinecraftClient client;
	private final WorldSession worldSession;
	private final AdvancementManager manager = new AdvancementManager();
	private final Map<AdvancementEntry, AdvancementProgress> advancementProgresses = new Object2ObjectOpenHashMap<>();
	private ClientAdvancementManager.@Nullable Listener listener;
	private @Nullable AdvancementEntry selectedTab;

	/**
	 * @param client       клиент Minecraft
	 * @param worldSession сессия мира для телеметрии достижений
	 */
	public ClientAdvancementManager(MinecraftClient client, WorldSession worldSession) {
		this.client = client;
		this.worldSession = worldSession;
	}

	/**
	 * Обрабатывает пакет обновления достижений от сервера.
	 * При необходимости очищает текущее состояние, добавляет новые достижения
	 * и обновляет прогресс по каждому из них.
	 *
	 * @param packet пакет с обновлениями достижений
	 */
	public void onAdvancements(AdvancementUpdateS2CPacket packet) {
		if (packet.shouldClearCurrent()) {
			manager.clear();
			advancementProgresses.clear();
		}

		manager.removeAll(packet.getAdvancementIdsToRemove());
		manager.addAll(packet.getAdvancementsToEarn());

		for (Entry<Identifier, AdvancementProgress> entry : packet.getAdvancementsToProgress().entrySet()) {
			PlacedAdvancement placed = manager.get(entry.getKey());

			if (placed == null) {
				LOGGER.warn("Server informed client about progress for unknown advancement {}", entry.getKey());
				continue;
			}

			AdvancementProgress progress = entry.getValue();
			progress.init(placed.getAdvancement().requirements());
			advancementProgresses.put(placed.getAdvancementEntry(), progress);

			if (listener != null) {
				listener.setProgress(placed, progress);
			}

			if (packet.shouldClearCurrent() || progress.isDone() == false) {
				continue;
			}

			if (client.world != null) {
				worldSession.onAdvancementMade(client.world, placed.getAdvancementEntry());
			}

			Optional<AdvancementDisplay> display = placed.getAdvancement().display();
			if (packet.shouldShowToast() && display.isPresent() && display.get().shouldShowToast()) {
				client.getToastManager().add(new AdvancementToast(placed.getAdvancementEntry()));
			}
		}
	}

	/**
	 * Возвращает менеджер дерева достижений.
	 *
	 * @return менеджер достижений
	 */
	public AdvancementManager getManager() {
		return manager;
	}

	/**
	 * Выбирает активную вкладку достижений.
	 * Если вкладка выбрана локально, отправляет пакет на сервер.
	 *
	 * @param tab   выбранное достижение-вкладка или {@code null} для сброса
	 * @param local {@code true} если выбор сделан пользователем (нужно уведомить сервер)
	 */
	public void selectTab(@Nullable AdvancementEntry tab, boolean local) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler != null && tab != null && local) {
			networkHandler.sendPacket(AdvancementTabC2SPacket.open(tab));
		}

		if (selectedTab == tab) {
			return;
		}

		selectedTab = tab;
		if (listener != null) {
			listener.selectTab(tab);
		}
	}

	/**
	 * Устанавливает слушателя изменений достижений.
	 * При установке нового слушателя немедленно воспроизводит текущее состояние.
	 *
	 * @param listener новый слушатель или {@code null} для отписки
	 */
	public void setListener(ClientAdvancementManager.@Nullable Listener listener) {
		this.listener = listener;
		manager.setListener(listener);

		if (listener == null) {
			return;
		}

		advancementProgresses.forEach((advancement, progress) -> {
			PlacedAdvancement placed = manager.get(advancement);
			if (placed != null) {
				listener.setProgress(placed, progress);
			}
		});
		listener.selectTab(selectedTab);
	}

	/**
	 * Возвращает запись достижения по его идентификатору.
	 *
	 * @param id идентификатор достижения
	 * @return запись достижения или {@code null} если не найдено
	 */
	public @Nullable AdvancementEntry get(Identifier id) {
		PlacedAdvancement placed = manager.get(id);
		return placed != null ? placed.getAdvancementEntry() : null;
	}

	/**
	 * Слушатель событий менеджера достижений на стороне клиента.
	 */
	@Environment(EnvType.CLIENT)
	public interface Listener extends AdvancementManager.Listener {

		/**
		 * Вызывается при обновлении прогресса достижения.
		 *
		 * @param advancement достижение
		 * @param progress    новый прогресс
		 */
		void setProgress(PlacedAdvancement advancement, AdvancementProgress progress);

		/**
		 * Вызывается при смене активной вкладки достижений.
		 *
		 * @param advancement выбранная вкладка или {@code null}
		 */
		void selectTab(@Nullable AdvancementEntry advancement);
	}
}
