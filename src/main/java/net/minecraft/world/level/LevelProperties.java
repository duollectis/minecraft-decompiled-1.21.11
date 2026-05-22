package net.minecraft.world.level;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.*;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.storage.SaveVersionInfo;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.timer.Timer;
import net.minecraft.world.timer.TimerCallbackSerializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Главный контейнер всех свойств сохранённого мира (level.dat).
 * Реализует {@link ServerWorldProperties} и {@link SaveProperties}, объединяя
 * базовые настройки уровня ({@link LevelInfo}), параметры генерации мира,
 * состояние погоды, таймер событий и прочие серверные данные.
 *
 * <p>Экземпляр создаётся либо через публичный конструктор (новый мир),
 * либо через {@link #readProperties} при загрузке существующего сохранения.
 */
public class LevelProperties implements ServerWorldProperties, SaveProperties {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final String LEVEL_NAME_KEY = "LevelName";
	protected static final String PLAYER_KEY = "Player";
	protected static final String WORLD_GEN_SETTINGS_KEY = "WorldGenSettings";

	/** Версия формата Anvil, используемая при записи level.dat. */
	private static final int ANVIL_FORMAT_VERSION = 19133;

	private LevelInfo levelInfo;
	private final GeneratorOptions generatorOptions;
	private final SpecialProperty specialProperty;
	private final Lifecycle lifecycle;
	private WorldProperties.SpawnPoint spawnPoint;
	private long time;
	private long timeOfDay;
	private final @Nullable NbtCompound playerData;
	private final int version;
	private int clearWeatherTime;
	private boolean raining;
	private int rainTime;
	private boolean thundering;
	private int thunderTime;
	private boolean initialized;
	private boolean difficultyLocked;
	@Deprecated
	private Optional<WorldBorder.Properties> worldBorder;
	private EnderDragonFight.Data dragonFight;
	private @Nullable NbtCompound customBossEvents;
	private int wanderingTraderSpawnDelay;
	private int wanderingTraderSpawnChance;
	private @Nullable UUID wanderingTraderId;
	private final Set<String> serverBrands;
	private boolean modded;
	private final Set<String> removedFeatures;
	private final Timer<MinecraftServer> scheduledEvents;

	private LevelProperties(
		@Nullable NbtCompound playerData,
		boolean modded,
		WorldProperties.SpawnPoint spawnPoint,
		long time,
		long timeOfDay,
		int version,
		int clearWeatherTime,
		int rainTime,
		boolean raining,
		int thunderTime,
		boolean thundering,
		boolean initialized,
		boolean difficultyLocked,
		Optional<WorldBorder.Properties> worldBorder,
		int wanderingTraderSpawnDelay,
		int wanderingTraderSpawnChance,
		@Nullable UUID wanderingTraderId,
		Set<String> serverBrands,
		Set<String> removedFeatures,
		Timer<MinecraftServer> scheduledEvents,
		@Nullable NbtCompound customBossEvents,
		EnderDragonFight.Data dragonFight,
		LevelInfo levelInfo,
		GeneratorOptions generatorOptions,
		SpecialProperty specialProperty,
		Lifecycle lifecycle
	) {
		this.modded = modded;
		this.spawnPoint = spawnPoint;
		this.time = time;
		this.timeOfDay = timeOfDay;
		this.version = version;
		this.clearWeatherTime = clearWeatherTime;
		this.rainTime = rainTime;
		this.raining = raining;
		this.thunderTime = thunderTime;
		this.thundering = thundering;
		this.initialized = initialized;
		this.difficultyLocked = difficultyLocked;
		this.worldBorder = worldBorder;
		this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
		this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
		this.wanderingTraderId = wanderingTraderId;
		this.serverBrands = serverBrands;
		this.removedFeatures = removedFeatures;
		this.playerData = playerData;
		this.scheduledEvents = scheduledEvents;
		this.customBossEvents = customBossEvents;
		this.dragonFight = dragonFight;
		this.levelInfo = levelInfo;
		this.generatorOptions = generatorOptions;
		this.specialProperty = specialProperty;
		this.lifecycle = lifecycle;
	}

	/** Создаёт свойства нового мира с дефолтными значениями всех полей. */
	public LevelProperties(
		LevelInfo levelInfo,
		GeneratorOptions generatorOptions,
		SpecialProperty specialProperty,
		Lifecycle lifecycle
	) {
		this(
			null,
			false,
			WorldProperties.SpawnPoint.DEFAULT,
			0L,
			0L,
			ANVIL_FORMAT_VERSION,
			0,
			0,
			false,
			0,
			false,
			false,
			false,
			Optional.empty(),
			0,
			0,
			null,
			Sets.newLinkedHashSet(),
			new HashSet<>(),
			new Timer<>(TimerCallbackSerializer.INSTANCE),
			null,
			EnderDragonFight.Data.DEFAULT,
			levelInfo.withCopiedGameRules(),
			generatorOptions,
			specialProperty,
			lifecycle
		);
	}

	/**
	 * Десериализует полные свойства мира из NBT-совместимого {@link Dynamic}.
	 * Применяется при загрузке существующего сохранения после прохождения DataFixer.
	 *
	 * @param dynamic           обновлённое динамическое представление данных уровня
	 * @param info              базовые настройки уровня (имя, режим, сложность и т.д.)
	 * @param specialProperty   тип особого мира (обычный, плоский, отладочный)
	 * @param generatorOptions  параметры генератора мира
	 * @param lifecycle         жизненный цикл (stable/experimental) для регистров
	 * @return полностью заполненный экземпляр {@code LevelProperties}
	 */
	public static <T> LevelProperties readProperties(
		Dynamic<T> dynamic,
		LevelInfo info,
		SpecialProperty specialProperty,
		GeneratorOptions generatorOptions,
		Lifecycle lifecycle
	) {
		long time = dynamic.get("Time").asLong(0L);

		return new LevelProperties(
			dynamic.get("Player").flatMap(NbtCompound.CODEC::parse).result().orElse(null),
			dynamic.get("WasModded").asBoolean(false),
			dynamic.get("spawn")
				.read(WorldProperties.SpawnPoint.CODEC)
				.result()
				.orElse(WorldProperties.SpawnPoint.DEFAULT),
			time,
			dynamic.get("DayTime").asLong(time),
			SaveVersionInfo.fromDynamic(dynamic).getLevelFormatVersion(),
			dynamic.get("clearWeatherTime").asInt(0),
			dynamic.get("rainTime").asInt(0),
			dynamic.get("raining").asBoolean(false),
			dynamic.get("thunderTime").asInt(0),
			dynamic.get("thundering").asBoolean(false),
			dynamic.get("initialized").asBoolean(true),
			dynamic.get("DifficultyLocked").asBoolean(false),
			WorldBorder.Properties.CODEC.parse(dynamic.get("world_border").orElseEmptyMap()).result(),
			dynamic.get("WanderingTraderSpawnDelay").asInt(0),
			dynamic.get("WanderingTraderSpawnChance").asInt(0),
			dynamic.get("WanderingTraderId").read(Uuids.INT_STREAM_CODEC).result().orElse(null),
			dynamic.get("ServerBrands")
				.asStream()
				.flatMap(brand -> brand.asString().result().stream())
				.collect(Collectors.toCollection(Sets::newLinkedHashSet)),
			dynamic.get("removed_features")
				.asStream()
				.flatMap(feature -> feature.asString().result().stream())
				.collect(Collectors.toSet()),
			new Timer<>(TimerCallbackSerializer.INSTANCE, dynamic.get("ScheduledEvents").asStream()),
			(NbtCompound) dynamic.get("CustomBossEvents").orElseEmptyMap().getValue(),
			dynamic.get("DragonFight")
				.read(EnderDragonFight.Data.CODEC)
				.resultOrPartial(LOGGER::error)
				.orElse(EnderDragonFight.Data.DEFAULT),
			info,
			generatorOptions,
			specialProperty,
			lifecycle
		);
	}

	@Override
	public NbtCompound cloneWorldNbt(DynamicRegistryManager registryManager, @Nullable NbtCompound playerNbt) {
		NbtCompound effectivePlayerNbt = playerNbt == null ? playerData : playerNbt;

		NbtCompound root = new NbtCompound();
		updateProperties(registryManager, root, effectivePlayerNbt);

		return root;
	}

	/**
	 * Сериализует все свойства мира в переданный NBT-компаунд.
	 * Метод записывает версию игры, настройки генерации, погоду, правила игры
	 * и все прочие поля level.dat.
	 */
	private void updateProperties(
		DynamicRegistryManager registryManager,
		NbtCompound levelNbt,
		@Nullable NbtCompound playerNbt
	) {
		levelNbt.put("ServerBrands", createStringList(serverBrands));
		levelNbt.putBoolean("WasModded", modded);

		if (!removedFeatures.isEmpty()) {
			levelNbt.put("removed_features", createStringList(removedFeatures));
		}

		NbtCompound versionNbt = new NbtCompound();
		versionNbt.putString("Name", SharedConstants.getGameVersion().name());
		versionNbt.putInt("Id", SharedConstants.getGameVersion().dataVersion().id());
		versionNbt.putBoolean("Snapshot", !SharedConstants.getGameVersion().stable());
		versionNbt.putString("Series", SharedConstants.getGameVersion().dataVersion().series());
		levelNbt.put("Version", versionNbt);

		NbtHelper.putDataVersion(levelNbt);

		DynamicOps<NbtElement> dynamicOps = registryManager.getOps(NbtOps.INSTANCE);
		WorldGenSettings.encode(dynamicOps, generatorOptions, registryManager)
			.resultOrPartial(Util.addPrefix("WorldGenSettings: ", LOGGER::error))
			.ifPresent(worldGenSettings -> levelNbt.put("WorldGenSettings", worldGenSettings));

		levelNbt.putInt("GameType", levelInfo.getGameMode().getIndex());
		levelNbt.put("spawn", WorldProperties.SpawnPoint.CODEC, spawnPoint);
		levelNbt.putLong("Time", time);
		levelNbt.putLong("DayTime", timeOfDay);
		levelNbt.putLong("LastPlayed", Util.getEpochTimeMs());
		levelNbt.putString("LevelName", levelInfo.getLevelName());
		levelNbt.putInt("version", ANVIL_FORMAT_VERSION);
		levelNbt.putInt("clearWeatherTime", clearWeatherTime);
		levelNbt.putInt("rainTime", rainTime);
		levelNbt.putBoolean("raining", raining);
		levelNbt.putInt("thunderTime", thunderTime);
		levelNbt.putBoolean("thundering", thundering);
		levelNbt.putBoolean("hardcore", levelInfo.isHardcore());
		levelNbt.putBoolean("allowCommands", levelInfo.areCommandsAllowed());
		levelNbt.putBoolean("initialized", initialized);

		worldBorder.ifPresent(border -> levelNbt.put("world_border", WorldBorder.Properties.CODEC, border));

		levelNbt.putByte("Difficulty", (byte) levelInfo.getDifficulty().getId());
		levelNbt.putBoolean("DifficultyLocked", difficultyLocked);
		levelNbt.put("game_rules", GameRules.createCodec(getEnabledFeatures()), levelInfo.getGameRules());
		levelNbt.put("DragonFight", EnderDragonFight.Data.CODEC, dragonFight);

		if (playerNbt != null) {
			levelNbt.put("Player", playerNbt);
		}

		levelNbt.copyFromCodec(DataConfiguration.MAP_CODEC, levelInfo.getDataConfiguration());

		if (customBossEvents != null) {
			levelNbt.put("CustomBossEvents", customBossEvents);
		}

		levelNbt.put("ScheduledEvents", scheduledEvents.toNbt());
		levelNbt.putInt("WanderingTraderSpawnDelay", wanderingTraderSpawnDelay);
		levelNbt.putInt("WanderingTraderSpawnChance", wanderingTraderSpawnChance);
		levelNbt.putNullable("WanderingTraderId", Uuids.INT_STREAM_CODEC, wanderingTraderId);
	}

	private static NbtList createStringList(Set<String> strings) {
		NbtList list = new NbtList();
		strings.stream().map(NbtString::of).forEach(list::add);
		return list;
	}

	@Override
	public WorldProperties.SpawnPoint getSpawnPoint() {
		return spawnPoint;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public long getTimeOfDay() {
		return timeOfDay;
	}

	@Override
	public @Nullable NbtCompound getPlayerData() {
		return playerData;
	}

	@Override
	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public void setTimeOfDay(long timeOfDay) {
		this.timeOfDay = timeOfDay;
	}

	@Override
	public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
		this.spawnPoint = spawnPoint;
	}

	@Override
	public String getLevelName() {
		return levelInfo.getLevelName();
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public int getClearWeatherTime() {
		return clearWeatherTime;
	}

	@Override
	public void setClearWeatherTime(int clearWeatherTime) {
		this.clearWeatherTime = clearWeatherTime;
	}

	@Override
	public boolean isThundering() {
		return thundering;
	}

	@Override
	public void setThundering(boolean thundering) {
		this.thundering = thundering;
	}

	@Override
	public int getThunderTime() {
		return thunderTime;
	}

	@Override
	public void setThunderTime(int thunderTime) {
		this.thunderTime = thunderTime;
	}

	@Override
	public boolean isRaining() {
		return raining;
	}

	@Override
	public void setRaining(boolean raining) {
		this.raining = raining;
	}

	@Override
	public int getRainTime() {
		return rainTime;
	}

	@Override
	public void setRainTime(int rainTime) {
		this.rainTime = rainTime;
	}

	@Override
	public GameMode getGameMode() {
		return levelInfo.getGameMode();
	}

	@Override
	public void setGameMode(GameMode gameMode) {
		levelInfo = levelInfo.withGameMode(gameMode);
	}

	@Override
	public boolean isHardcore() {
		return levelInfo.isHardcore();
	}

	@Override
	public boolean areCommandsAllowed() {
		return levelInfo.areCommandsAllowed();
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	@Override
	public GameRules getGameRules() {
		return levelInfo.getGameRules();
	}

	@Override
	public Optional<WorldBorder.Properties> getWorldBorder() {
		return worldBorder;
	}

	@Override
	public void setWorldBorder(Optional<WorldBorder.Properties> worldBorder) {
		this.worldBorder = worldBorder;
	}

	@Override
	public Difficulty getDifficulty() {
		return levelInfo.getDifficulty();
	}

	@Override
	public void setDifficulty(Difficulty difficulty) {
		levelInfo = levelInfo.withDifficulty(difficulty);
	}

	@Override
	public boolean isDifficultyLocked() {
		return difficultyLocked;
	}

	@Override
	public void setDifficultyLocked(boolean difficultyLocked) {
		this.difficultyLocked = difficultyLocked;
	}

	@Override
	public Timer<MinecraftServer> getScheduledEvents() {
		return scheduledEvents;
	}

	@Override
	public void populateCrashReport(CrashReportSection reportSection, HeightLimitView world) {
		ServerWorldProperties.super.populateCrashReport(reportSection, world);
		SaveProperties.super.populateCrashReport(reportSection);
	}

	@Override
	public GeneratorOptions getGeneratorOptions() {
		return generatorOptions;
	}

	@Override
	public boolean isFlatWorld() {
		return specialProperty == SpecialProperty.FLAT;
	}

	@Override
	public boolean isDebugWorld() {
		return specialProperty == SpecialProperty.DEBUG;
	}

	@Override
	public Lifecycle getLifecycle() {
		return lifecycle;
	}

	@Override
	public EnderDragonFight.Data getDragonFight() {
		return dragonFight;
	}

	@Override
	public void setDragonFight(EnderDragonFight.Data dragonFight) {
		this.dragonFight = dragonFight;
	}

	@Override
	public DataConfiguration getDataConfiguration() {
		return levelInfo.getDataConfiguration();
	}

	@Override
	public void updateLevelInfo(DataConfiguration dataConfiguration) {
		levelInfo = levelInfo.withDataConfiguration(dataConfiguration);
	}

	@Override
	public @Nullable NbtCompound getCustomBossEvents() {
		return customBossEvents;
	}

	@Override
	public void setCustomBossEvents(@Nullable NbtCompound customBossEvents) {
		this.customBossEvents = customBossEvents;
	}

	@Override
	public int getWanderingTraderSpawnDelay() {
		return wanderingTraderSpawnDelay;
	}

	@Override
	public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
		this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
	}

	@Override
	public int getWanderingTraderSpawnChance() {
		return wanderingTraderSpawnChance;
	}

	@Override
	public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
		this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
	}

	@Override
	public @Nullable UUID getWanderingTraderId() {
		return wanderingTraderId;
	}

	@Override
	public void setWanderingTraderId(UUID wanderingTraderId) {
		this.wanderingTraderId = wanderingTraderId;
	}

	@Override
	public void addServerBrand(String brand, boolean isModded) {
		serverBrands.add(brand);
		modded |= isModded;
	}

	@Override
	public boolean isModded() {
		return modded;
	}

	@Override
	public Set<String> getServerBrands() {
		return ImmutableSet.copyOf(serverBrands);
	}

	@Override
	public Set<String> getRemovedFeatures() {
		return Set.copyOf(removedFeatures);
	}

	@Override
	public ServerWorldProperties getMainWorldProperties() {
		return this;
	}

	@Override
	public LevelInfo getLevelInfo() {
		return levelInfo.withCopiedGameRules();
	}

	/**
	 * Тип особого мира, влияющий на логику генерации и отображение в интерфейсе.
	 * {@code NONE} — обычный мир, {@code FLAT} — суперплоский, {@code DEBUG} — отладочный.
	 */
	public enum SpecialProperty {
		NONE,
		FLAT,
		DEBUG
	}
}
