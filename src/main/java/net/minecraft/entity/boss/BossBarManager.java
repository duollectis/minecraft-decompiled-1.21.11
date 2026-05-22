package net.minecraft.entity.boss;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * Серверный менеджер командных полос здоровья боссов ({@link CommandBossBar}).
 * Хранит все активные boss bar'ы, созданные через команду {@code /bossbar},
 * и обеспечивает их сериализацию в NBT и синхронизацию при подключении/отключении игроков.
 */
public class BossBarManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Codec<Map<Identifier, CommandBossBar.Serialized>> CODEC =
			Codec.unboundedMap(Identifier.CODEC, CommandBossBar.Serialized.CODEC);

	private final Map<Identifier, CommandBossBar> commandBossBars = Maps.newHashMap();

	public @Nullable CommandBossBar get(Identifier id) {
		return commandBossBars.get(id);
	}

	public CommandBossBar add(Identifier id, Text displayName) {
		CommandBossBar bossBar = new CommandBossBar(id, displayName);
		commandBossBars.put(id, bossBar);
		return bossBar;
	}

	public void remove(CommandBossBar bossBar) {
		commandBossBars.remove(bossBar.getId());
	}

	public Collection<Identifier> getIds() {
		return commandBossBars.keySet();
	}

	public Collection<CommandBossBar> getAll() {
		return commandBossBars.values();
	}

	/**
	 * Сериализует все boss bar'ы в NBT для сохранения на диск.
	 */
	public NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
		Map<Identifier, CommandBossBar.Serialized> serialized =
				Util.transformMapValues(commandBossBars, CommandBossBar::toSerialized);
		return (NbtCompound) CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), serialized).getOrThrow();
	}

	/**
	 * Восстанавливает boss bar'ы из NBT. При ошибке парсинга логирует проблему и пропускает повреждённые записи.
	 */
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		Map<Identifier, CommandBossBar.Serialized> parsed = CODEC
				.parse(registries.getOps(NbtOps.INSTANCE), nbt)
				.resultOrPartial(error -> LOGGER.error("Failed to parse boss bar events: {}", error))
				.orElse(Map.of());

		parsed.forEach((id, serialized) -> commandBossBars.put(id, CommandBossBar.fromSerialized(id, serialized)));
	}

	public void onPlayerConnect(ServerPlayerEntity player) {
		for (CommandBossBar bossBar : commandBossBars.values()) {
			bossBar.onPlayerConnect(player);
		}
	}

	public void onPlayerDisconnect(ServerPlayerEntity player) {
		for (CommandBossBar bossBar : commandBossBars.values()) {
			bossBar.onPlayerDisconnect(player);
		}
	}
}
