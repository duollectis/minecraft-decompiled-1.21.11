package net.minecraft.world.debug.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.Memory;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Полный снимок состояния «мозга» живой сущности для отладочной визуализации на клиенте.
 * <p>
 * Содержит имя, профессию, здоровье, инвентарь, активные поведения, воспоминания,
 * сплетни (для жителей), а также позиции точек интереса. Сериализуется вручную
 * через {@link PacketByteBuf}, поскольку структура данных слишком сложна для
 * автоматической кодогенерации через {@code PacketCodec.tuple}.
 */
public record BrainDebugData(
		String name,
		String profession,
		int xp,
		float health,
		float maxHealth,
		String inventory,
		boolean wantsGolem,
		int angerLevel,
		List<String> activities,
		List<String> behaviors,
		List<String> memories,
		List<String> gossips,
		Set<BlockPos> pois,
		Set<BlockPos> potentialPois
) {

	/** Максимальная длина строки одного воспоминания при передаче по сети. */
	private static final int MEMORY_STRING_MAX_LENGTH = 255;

	/** Значение angerLevel для сущностей, не поддерживающих систему гнева. */
	private static final int ANGER_LEVEL_NONE = -1;

	public static final PacketCodec<PacketByteBuf, BrainDebugData> PACKET_CODEC = PacketCodec.ofStatic(
			(buf, data) -> data.write(buf),
			BrainDebugData::new
	);

	public BrainDebugData(PacketByteBuf buf) {
		this(
				buf.readString(),
				buf.readString(),
				buf.readInt(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readString(),
				buf.readBoolean(),
				buf.readInt(),
				buf.readList(PacketByteBuf::readString),
				buf.readList(PacketByteBuf::readString),
				buf.readList(PacketByteBuf::readString),
				buf.readList(PacketByteBuf::readString),
				buf.readCollection(HashSet::new, BlockPos.PACKET_CODEC),
				buf.readCollection(HashSet::new, BlockPos.PACKET_CODEC)
		);
	}

	public void write(PacketByteBuf buf) {
		buf.writeString(name);
		buf.writeString(profession);
		buf.writeInt(xp);
		buf.writeFloat(health);
		buf.writeFloat(maxHealth);
		buf.writeString(inventory);
		buf.writeBoolean(wantsGolem);
		buf.writeInt(angerLevel);
		buf.writeCollection(activities, PacketByteBuf::writeString);
		buf.writeCollection(behaviors, PacketByteBuf::writeString);
		buf.writeCollection(memories, PacketByteBuf::writeString);
		buf.writeCollection(gossips, PacketByteBuf::writeString);
		buf.writeCollection(pois, BlockPos.PACKET_CODEC);
		buf.writeCollection(potentialPois, BlockPos.PACKET_CODEC);
	}

	/**
	 * Создаёт снимок состояния мозга сущности в текущий момент времени.
	 * <p>
	 * Для жителей извлекаются профессия, опыт, желание призвать голема и сплетни.
	 * Для Warden — уровень гнева. Для остальных сущностей эти поля принимают
	 * нейтральные значения (пустая строка, 0, {@value #ANGER_LEVEL_NONE}).
	 *
	 * @param world  серверный мир, используется для разрешения UUID в сущности
	 * @param entity живая сущность, чей мозг снимается
	 * @return снимок состояния мозга
	 */
	public static BrainDebugData fromEntity(ServerWorld world, LivingEntity entity) {
		String entityName = NameGenerator.name(entity);

		String profession = "";
		int xp = 0;
		if (entity instanceof VillagerEntity villager) {
			profession = villager.getVillagerData().profession().getIdAsString();
			xp = villager.getExperience();
		}

		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		Brain<?> brain = entity.getBrain();
		long worldTime = entity.getEntityWorld().getTime();

		String inventoryStr = "";
		if (entity instanceof InventoryOwner inventoryOwner) {
			Inventory inventory = inventoryOwner.getInventory();
			inventoryStr = inventory.isEmpty() ? "" : inventory.toString();
		}

		boolean wantsGolem = entity instanceof VillagerEntity villager && villager.canSummonGolem(worldTime);
		int angerLevel = entity instanceof WardenEntity warden ? warden.getAnger() : ANGER_LEVEL_NONE;

		List<String> activities = brain.getPossibleActivities().stream().map(Activity::getId).toList();
		List<String> behaviors = brain.getRunningTasks().stream().map(Task::getName).toList();
		List<String> memories = streamMemories(world, entity, worldTime)
				.map(memory -> StringHelper.truncate(memory, MEMORY_STRING_MAX_LENGTH, true))
				.toList();

		Set<BlockPos> pois = getMemorizedPositions(
				brain,
				MemoryModuleType.JOB_SITE,
				MemoryModuleType.HOME,
				MemoryModuleType.MEETING_POINT
		);
		Set<BlockPos> potentialPois = getMemorizedPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);

		List<String> gossips = entity instanceof VillagerEntity villager
				? getGossips(villager)
				: List.of();

		return new BrainDebugData(
				entityName, profession, xp, health, maxHealth,
				inventoryStr, wantsGolem, angerLevel,
				activities, behaviors, memories, gossips, pois, potentialPois
		);
	}

	public boolean poiContains(BlockPos pos) {
		return pois.contains(pos);
	}

	public boolean potentialPoiContains(BlockPos pos) {
		return potentialPois.contains(pos);
	}

	@SafeVarargs
	private static Set<BlockPos> getMemorizedPositions(Brain<?> brain, MemoryModuleType<GlobalPos>... types) {
		return Stream.of(types)
		             .filter(brain::hasMemoryModule)
		             .map(brain::getOptionalRegisteredMemory)
		             .flatMap(Optional::stream)
		             .map(GlobalPos::pos)
		             .collect(Collectors.toSet());
	}

	private static List<String> getGossips(VillagerEntity villager) {
		List<String> result = new ArrayList<>();
		villager.getGossip().getEntityReputationAssociatedGossips().forEach((uuid, gossips) -> {
			String name = NameGenerator.name(uuid);
			gossips.forEach((type, value) -> result.add(name + ": " + type + ": " + value));
		});
		return result;
	}

	private static Stream<String> streamMemories(ServerWorld world, LivingEntity entity, long time) {
		return entity.getBrain()
		             .getMemories()
		             .entrySet()
		             .stream()
		             .map(entry -> collectMemoryString(world, time, entry.getKey(), entry.getValue()))
		             .sorted();
	}

	/**
	 * Форматирует одно воспоминание в читаемую строку вида {@code "ключ: значение"}.
	 * <p>
	 * Для {@link MemoryModuleType#HEARD_BELL_TIME} вычисляется разница с текущим временем.
	 * Для временных воспоминаний добавляется TTL. Отсутствующее воспоминание отображается как {@code "-"}.
	 */
	private static String collectMemoryString(
			ServerWorld world,
			long time,
			MemoryModuleType<?> type,
			Optional<? extends Memory<?>> memory
	) {
		String value;
		if (memory.isPresent()) {
			Memory<?> resolved = memory.get();
			Object rawValue = resolved.getValue();
			if (type == MemoryModuleType.HEARD_BELL_TIME) {
				long ticksAgo = time - (Long) rawValue;
				value = ticksAgo + " ticks ago";
			} else if (resolved.isTimed()) {
				value = toString(world, rawValue) + " (ttl: " + resolved.getExpiry() + ")";
			} else {
				value = toString(world, rawValue);
			}
		} else {
			value = "-";
		}

		return Registries.MEMORY_MODULE_TYPE.getId(type).getPath() + ": " + value;
	}

	private static String toString(ServerWorld world, @Nullable Object value) {
		return switch (value) {
			case null -> "-";
			case UUID uuid -> toString(world, world.getEntity(uuid));
			case Entity entity -> NameGenerator.name(entity);
			case WalkTarget walkTarget -> toString(world, walkTarget.getLookTarget());
			case EntityLookTarget entityLookTarget -> toString(world, entityLookTarget.getEntity());
			case GlobalPos globalPos -> toString(world, globalPos.pos());
			case BlockPosLookTarget blockPosLookTarget -> toString(world, blockPosLookTarget.getBlockPos());
			case DamageSource damageSource -> {
				Entity attacker = damageSource.getAttacker();
				yield attacker == null ? value.toString() : toString(world, attacker);
			}
			case Collection<?> collection ->
					"[" + collection.stream().map(v -> toString(world, v)).collect(Collectors.joining(", ")) + "]";
			default -> value.toString();
		};
	}
}
