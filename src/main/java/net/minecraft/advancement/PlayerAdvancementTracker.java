package net.minecraft.advancement;

import com.google.gson.*;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SelectAdvancementTabS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Отслеживает прогресс достижений конкретного игрока: хранит состояние критериев,
 * управляет видимостью достижений в дереве и синхронизирует данные с клиентом.
 */
public class PlayerAdvancementTracker {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// Версия формата сохранения прогресса достижений (DataFixer)
	private static final int ADVANCEMENTS_DATA_VERSION = 1343;

	private final PlayerManager playerManager;
	private final Path filePath;
	private AdvancementManager advancementManager;
	private final Map<AdvancementEntry, AdvancementProgress> progress = new LinkedHashMap<>();
	private final Set<AdvancementEntry> visibleAdvancements = new HashSet<>();
	private final Set<AdvancementEntry> progressUpdates = new HashSet<>();
	private final Set<PlacedAdvancement> updatedRoots = new HashSet<>();
	private ServerPlayerEntity owner;
	private @Nullable AdvancementEntry currentDisplayTab;
	private boolean dirty = true;
	private final Codec<ProgressMap> progressMapCodec;

	public PlayerAdvancementTracker(
			DataFixer dataFixer,
			PlayerManager playerManager,
			ServerAdvancementLoader advancementLoader,
			Path filePath,
			ServerPlayerEntity owner
	) {
		this.playerManager = playerManager;
		this.filePath = filePath;
		this.owner = owner;
		advancementManager = advancementLoader.getManager();
		progressMapCodec = DataFixTypes.ADVANCEMENTS.createDataFixingCodec(
				ProgressMap.CODEC,
				dataFixer,
				ADVANCEMENTS_DATA_VERSION
		);
		load(advancementLoader);
	}

	public void setOwner(ServerPlayerEntity owner) {
		this.owner = owner;
	}

	public void clearCriteria() {
		for (Criterion<?> criterion : Registries.CRITERION) {
			criterion.endTracking(this);
		}
	}

	public void reload(ServerAdvancementLoader advancementLoader) {
		clearCriteria();
		progress.clear();
		visibleAdvancements.clear();
		updatedRoots.clear();
		progressUpdates.clear();
		dirty = true;
		currentDisplayTab = null;
		advancementManager = advancementLoader.getManager();
		load(advancementLoader);
	}

	private void beginTrackingAllAdvancements(ServerAdvancementLoader advancementLoader) {
		for (AdvancementEntry entry : advancementLoader.getAdvancements()) {
			beginTracking(entry);
		}
	}

	private void rewardEmptyAdvancements(ServerAdvancementLoader advancementLoader) {
		for (AdvancementEntry entry : advancementLoader.getAdvancements()) {
			Advancement advancement = entry.value();
			if (advancement.criteria().isEmpty()) {
				grantCriterion(entry, "");
				advancement.rewards().apply(owner);
			}
		}
	}

	private void load(ServerAdvancementLoader advancementLoader) {
		if (Files.isRegularFile(filePath)) {
			try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
				JsonElement json = StrictJsonParser.parse(reader);
				ProgressMap progressMap = progressMapCodec
						.parse(JsonOps.INSTANCE, json)
						.getOrThrow(JsonParseException::new);
				loadProgressMap(advancementLoader, progressMap);
			} catch (JsonIOException | IOException ex) {
				LOGGER.error("Couldn't access player advancements in {}", filePath, ex);
			} catch (JsonParseException ex) {
				LOGGER.error("Couldn't parse player advancements in {}", filePath, ex);
			}
		}

		rewardEmptyAdvancements(advancementLoader);
		beginTrackingAllAdvancements(advancementLoader);
	}

	public void save() {
		JsonElement json = progressMapCodec
				.encodeStart(JsonOps.INSTANCE, createProgressMap())
				.getOrThrow();

		try {
			PathUtil.createDirectories(filePath.getParent());

			try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
				GSON.toJson(json, GSON.newJsonWriter(writer));
			}
		} catch (JsonIOException | IOException ex) {
			LOGGER.error("Couldn't save player advancements to {}", filePath, ex);
		}
	}

	private void loadProgressMap(ServerAdvancementLoader loader, ProgressMap progressMap) {
		progressMap.forEach((id, advancementProgress) -> {
			AdvancementEntry entry = loader.get(id);
			if (entry == null) {
				LOGGER.warn(
						"Ignored advancement '{}' in progress file {} - it doesn't exist anymore?",
						id,
						filePath
				);
				return;
			}

			initProgress(entry, advancementProgress);
			progressUpdates.add(entry);
			onStatusUpdate(entry);
		});
	}

	private ProgressMap createProgressMap() {
		Map<Identifier, AdvancementProgress> result = new LinkedHashMap<>();
		progress.forEach((entry, advancementProgress) -> {
			if (advancementProgress.isAnyObtained()) {
				result.put(entry.id(), advancementProgress);
			}
		});
		return new ProgressMap(result);
	}

	/**
	 * Выдаёт критерий достижения игроку. Если достижение выполнено полностью —
	 * применяет награды и рассылает объявление в чат (если включено правилом игры).
	 *
	 * @param advancement   достижение, которому принадлежит критерий
	 * @param criterionName имя критерия
	 * @return {@code true}, если критерий был успешно выдан
	 */
	public boolean grantCriterion(AdvancementEntry advancement, String criterionName) {
		AdvancementProgress advancementProgress = getProgress(advancement);
		boolean wasAlreadyDone = advancementProgress.isDone();

		if (!advancementProgress.obtain(criterionName)) {
			return false;
		}

		endTrackingCompleted(advancement);
		progressUpdates.add(advancement);

		if (!wasAlreadyDone && advancementProgress.isDone()) {
			advancement.value().rewards().apply(owner);
			advancement.value().display().ifPresent(display -> {
				if (display.shouldAnnounceToChat()
						&& owner.getEntityWorld().getGameRules().getValue(GameRules.ANNOUNCE_ADVANCEMENTS)) {
					playerManager.broadcast(
							display.getFrame().getChatAnnouncementText(advancement, owner),
							false
					);
				}
			});
		}

		if (!wasAlreadyDone && advancementProgress.isDone()) {
			onStatusUpdate(advancement);
		}

		return true;
	}

	/**
	 * Отзывает критерий достижения у игрока. Если достижение было выполнено —
	 * возобновляет отслеживание критериев.
	 *
	 * @param advancement   достижение, которому принадлежит критерий
	 * @param criterionName имя критерия
	 * @return {@code true}, если критерий был успешно отозван
	 */
	public boolean revokeCriterion(AdvancementEntry advancement, String criterionName) {
		AdvancementProgress advancementProgress = getProgress(advancement);
		boolean wasDone = advancementProgress.isDone();

		if (!advancementProgress.reset(criterionName)) {
			return false;
		}

		beginTracking(advancement);
		progressUpdates.add(advancement);

		if (wasDone && !advancementProgress.isDone()) {
			onStatusUpdate(advancement);
		}

		return true;
	}

	private void onStatusUpdate(AdvancementEntry advancement) {
		PlacedAdvancement placed = advancementManager.get(advancement);
		if (placed != null) {
			updatedRoots.add(placed.getRoot());
		}
	}

	private void beginTracking(AdvancementEntry advancement) {
		AdvancementProgress advancementProgress = getProgress(advancement);
		if (advancementProgress.isDone()) {
			return;
		}

		for (Entry<String, AdvancementCriterion<?>> entry : advancement.value().criteria().entrySet()) {
			CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
			if (criterionProgress != null && !criterionProgress.isObtained()) {
				beginTracking(advancement, entry.getKey(), entry.getValue());
			}
		}
	}

	private <T extends CriterionConditions> void beginTracking(
			AdvancementEntry advancement,
			String id,
			AdvancementCriterion<T> criterion
	) {
		criterion
				.trigger()
				.beginTrackingCondition(
						this,
						new Criterion.ConditionsContainer<>(criterion.conditions(), advancement, id)
				);
	}

	private void endTrackingCompleted(AdvancementEntry advancement) {
		AdvancementProgress advancementProgress = getProgress(advancement);

		for (Entry<String, AdvancementCriterion<?>> entry : advancement.value().criteria().entrySet()) {
			CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
			if (criterionProgress != null && (criterionProgress.isObtained() || advancementProgress.isDone())) {
				endTrackingCompleted(advancement, entry.getKey(), entry.getValue());
			}
		}
	}

	private <T extends CriterionConditions> void endTrackingCompleted(
			AdvancementEntry advancement,
			String id,
			AdvancementCriterion<T> criterion
	) {
		criterion
				.trigger()
				.endTrackingCondition(
						this,
						new Criterion.ConditionsContainer<>(criterion.conditions(), advancement, id)
				);
	}

	/**
	 * Отправляет накопленные обновления прогресса и видимости достижений клиенту.
	 * Вызывается каждый тик для синхронизации состояния.
	 *
	 * @param player    игрок-получатель пакета
	 * @param showToast показывать ли всплывающие уведомления о новых достижениях
	 */
	public void sendUpdate(ServerPlayerEntity player, boolean showToast) {
		if (!dirty && updatedRoots.isEmpty() && progressUpdates.isEmpty()) {
			dirty = false;
			return;
		}

		Map<Identifier, AdvancementProgress> progressToSend = new HashMap<>();
		Set<AdvancementEntry> added = new HashSet<>();
		Set<Identifier> removed = new HashSet<>();

		for (PlacedAdvancement root : updatedRoots) {
			calculateDisplay(root, added, removed);
		}

		updatedRoots.clear();

		for (AdvancementEntry entry : progressUpdates) {
			if (visibleAdvancements.contains(entry)) {
				progressToSend.put(entry.id(), progress.get(entry));
			}
		}

		progressUpdates.clear();

		if (!progressToSend.isEmpty() || !added.isEmpty() || !removed.isEmpty()) {
			player.networkHandler.sendPacket(
					new AdvancementUpdateS2CPacket(dirty, added, removed, progressToSend, showToast)
			);
		}

		dirty = false;
	}

	public void setDisplayTab(@Nullable AdvancementEntry advancement) {
		AdvancementEntry previous = currentDisplayTab;
		boolean isValidTab = advancement != null
				&& advancement.value().isRoot()
				&& advancement.value().display().isPresent();
		currentDisplayTab = isValidTab ? advancement : null;

		if (previous != currentDisplayTab) {
			owner.networkHandler.sendPacket(new SelectAdvancementTabS2CPacket(
					currentDisplayTab == null ? null : currentDisplayTab.id()
			));
		}
	}

	public AdvancementProgress getProgress(AdvancementEntry advancement) {
		AdvancementProgress existing = progress.get(advancement);
		if (existing != null) {
			return existing;
		}

		AdvancementProgress created = new AdvancementProgress();
		initProgress(advancement, created);
		return created;
	}

	private void initProgress(AdvancementEntry advancement, AdvancementProgress advancementProgress) {
		advancementProgress.init(advancement.value().requirements());
		progress.put(advancement, advancementProgress);
	}

	private void calculateDisplay(PlacedAdvancement root, Set<AdvancementEntry> added, Set<Identifier> removed) {
		AdvancementDisplays.calculateDisplay(
				root,
				advancement -> getProgress(advancement.getAdvancementEntry()).isDone(),
				(advancement, displayed) -> {
					AdvancementEntry entry = advancement.getAdvancementEntry();
					if (displayed) {
						if (visibleAdvancements.add(entry)) {
							added.add(entry);
							if (progress.containsKey(entry)) {
								progressUpdates.add(entry);
							}
						}
					} else if (visibleAdvancements.remove(entry)) {
						removed.add(entry.id());
					}
				}
		);
	}

	/**
	 * Обёртка над картой прогресса достижений для сериализации/десериализации.
	 * При итерации сортирует записи по значению (времени получения) для детерминированного порядка.
	 */
	record ProgressMap(Map<Identifier, AdvancementProgress> map) {

		public static final Codec<ProgressMap> CODEC = Codec.unboundedMap(
				Identifier.CODEC,
				AdvancementProgress.CODEC
		).xmap(ProgressMap::new, ProgressMap::map);

		public void forEach(BiConsumer<Identifier, AdvancementProgress> consumer) {
			map.entrySet()
					.stream()
					.sorted(Entry.comparingByValue())
					.forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
		}
	}
}
