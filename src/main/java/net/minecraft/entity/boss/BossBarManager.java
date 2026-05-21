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
 * {@code BossBarManager}.
 */
public class BossBarManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Codec<Map<Identifier, CommandBossBar.Serialized>>
			CODEC =
			Codec.unboundedMap(Identifier.CODEC, CommandBossBar.Serialized.CODEC);
	private final Map<Identifier, CommandBossBar> commandBossBars = Maps.newHashMap();

	/**
	 * Get.
	 *
	 * @param id id
	 *
	 * @return @Nullable CommandBossBar — 
	 */
	public @Nullable CommandBossBar get(Identifier id) {
		return this.commandBossBars.get(id);
	}

	/**
	 * Add.
	 *
	 * @param id id
	 * @param displayName display name
	 *
	 * @return CommandBossBar — результат операции
	 */
	public CommandBossBar add(Identifier id, Text displayName) {
		CommandBossBar commandBossBar = new CommandBossBar(id, displayName);
		this.commandBossBars.put(id, commandBossBar);
		return commandBossBar;
	}

	/**
	 * Remove.
	 *
	 * @param bossBar boss bar
	 */
	public void remove(CommandBossBar bossBar) {
		this.commandBossBars.remove(bossBar.getId());
	}

	public Collection<Identifier> getIds() {
		return this.commandBossBars.keySet();
	}

	public Collection<CommandBossBar> getAll() {
		return this.commandBossBars.values();
	}

	/**
	 * To nbt.
	 *
	 * @param registries registries
	 *
	 * @return NbtCompound — результат операции
	 */
	public NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
		Map<Identifier, CommandBossBar.Serialized>
				map =
				Util.transformMapValues(this.commandBossBars, CommandBossBar::toSerialized);
		return (NbtCompound) CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), map).getOrThrow();
	}

	/**
	 * Читает nbt.
	 *
	 * @param nbt nbt
	 * @param registries registries
	 */
	public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		Map<Identifier, CommandBossBar.Serialized> map = CODEC.parse(registries.getOps(NbtOps.INSTANCE), nbt)
		                                                      .resultOrPartial(error -> LOGGER.error(
				                                                      "Failed to parse boss bar events: {}",
				                                                      error
		                                                      ))
		                                                      .orElse(Map.of());
		map.forEach((id, serialized) -> this.commandBossBars.put(id, CommandBossBar.fromSerialized(id, serialized)));
	}

	/**
	 * Обрабатывает событие player connect.
	 *
	 * @param player player
	 */
	public void onPlayerConnect(ServerPlayerEntity player) {
		for (CommandBossBar commandBossBar : this.commandBossBars.values()) {
			commandBossBar.onPlayerConnect(player);
		}
	}

	/**
	 * Обрабатывает событие player disconnect.
	 *
	 * @param player player
	 */
	public void onPlayerDisconnect(ServerPlayerEntity player) {
		for (CommandBossBar commandBossBar : this.commandBossBars.values()) {
			commandBossBar.onPlayerDisconnect(player);
		}
	}
}
