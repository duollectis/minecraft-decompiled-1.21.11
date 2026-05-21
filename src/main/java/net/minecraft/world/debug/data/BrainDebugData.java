package net.minecraft.world.debug.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code BrainDebugData}.
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

	public static final PacketCodec<PacketByteBuf, BrainDebugData> PACKET_CODEC = PacketCodec.ofStatic(
			(packetByteBuf, brainDebugData) -> brainDebugData.write(packetByteBuf), BrainDebugData::new
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

	/**
	 * Write.
	 *
	 * @param buf buf
	 */
	public void write(PacketByteBuf buf) {
		buf.writeString(this.name);
		buf.writeString(this.profession);
		buf.writeInt(this.xp);
		buf.writeFloat(this.health);
		buf.writeFloat(this.maxHealth);
		buf.writeString(this.inventory);
		buf.writeBoolean(this.wantsGolem);
		buf.writeInt(this.angerLevel);
		buf.writeCollection(this.activities, PacketByteBuf::writeString);
		buf.writeCollection(this.behaviors, PacketByteBuf::writeString);
		buf.writeCollection(this.memories, PacketByteBuf::writeString);
		buf.writeCollection(this.gossips, PacketByteBuf::writeString);
		buf.writeCollection(this.pois, BlockPos.PACKET_CODEC);
		buf.writeCollection(this.potentialPois, BlockPos.PACKET_CODEC);
	}

	/**
	 * From entity.
	 *
	 * @param world world
	 * @param entity entity
	 *
	 * @return BrainDebugData — результат операции
	 */
	public static BrainDebugData fromEntity(ServerWorld world, LivingEntity entity) {
		String string = NameGenerator.name(entity);
		String string2;
		int i;
		if (entity instanceof VillagerEntity villagerEntity) {
			string2 = villagerEntity.getVillagerData().profession().getIdAsString();
			i = villagerEntity.getExperience();
		}
		else {
			string2 = "";
			i = 0;
		}

		float f = entity.getHealth();
		float g = entity.getMaxHealth();
		Brain<?> brain = entity.getBrain();
		long l = entity.getEntityWorld().getTime();
		String string3;
		if (entity instanceof InventoryOwner inventoryOwner) {
			Inventory inventory = inventoryOwner.getInventory();
			string3 = inventory.isEmpty() ? "" : inventory.toString();
		}
		else {
			string3 = "";
		}

		boolean bl = entity instanceof VillagerEntity villagerEntity2 && villagerEntity2.canSummonGolem(l);
		int j = entity instanceof WardenEntity wardenEntity ? wardenEntity.getAnger() : -1;
		List<String> list = brain.getPossibleActivities().stream().map(Activity::getId).toList();
		List<String> list2 = brain.getRunningTasks().stream().map(Task::getName).toList();
		List<String>
				list3 =
				streamMemories(world, entity, l).map(memory -> StringHelper.truncate(memory, 255, true)).toList();
		Set<BlockPos>
				set =
				getMemorizedPositions(
						brain,
						MemoryModuleType.JOB_SITE,
						MemoryModuleType.HOME,
						MemoryModuleType.MEETING_POINT
				);
		Set<BlockPos> set2 = getMemorizedPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);
		List<String> list4 = entity instanceof VillagerEntity villagerEntity3 ? getGossips(villagerEntity3) : List.of();
		return new BrainDebugData(string, string2, i, f, g, string3, bl, j, list, list2, list3, list4, set, set2);
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
		List<String> list = new ArrayList<>();
		villager.getGossip().getEntityReputationAssociatedGossips().forEach((uuid, gossips) -> {
			String string = NameGenerator.name(uuid);
			gossips.forEach((type, gossip) -> list.add(string + ": " + type + ": " + gossip));
		});
		return list;
	}

	private static Stream<String> streamMemories(ServerWorld world, LivingEntity entity, long time) {
		return entity.getBrain().getMemories().entrySet().stream().map(entry -> {
			MemoryModuleType<?> memoryModuleType = entry.getKey();
			Optional<? extends Memory<?>> optional = entry.getValue();
			return collectMemoryString(world, time, memoryModuleType, optional);
		}).sorted();
	}

	private static String collectMemoryString(
			ServerWorld world,
			long time,
			MemoryModuleType<?> type,
			Optional<? extends Memory<?>> memory
	) {
		String string;
		if (memory.isPresent()) {
			Memory<?> memory2 = (Memory<?>) memory.get();
			Object object = memory2.getValue();
			if (type == MemoryModuleType.HEARD_BELL_TIME) {
				long l = time - (Long) object;
				string = l + " ticks ago";
			}
			else if (memory2.isTimed()) {
				string = toString(world, object) + " (ttl: " + memory2.getExpiry() + ")";
			}
			else {
				string = toString(world, object);
			}
		}
		else {
			string = "-";
		}

		return Registries.MEMORY_MODULE_TYPE.getId(type).getPath() + ": " + string;
	}

	private static String toString(ServerWorld world, @Nullable Object value) {
		return switch (value) {
			case null -> "-";
			case UUID uUID -> toString(world, world.getEntity(uUID));
			case Entity entity -> NameGenerator.name(entity);
			case WalkTarget walkTarget -> toString(world, walkTarget.getLookTarget());
			case EntityLookTarget entityLookTarget -> toString(world, entityLookTarget.getEntity());
			case GlobalPos globalPos -> toString(world, globalPos.pos());
			case BlockPosLookTarget blockPosLookTarget -> toString(world, blockPosLookTarget.getBlockPos());
			case DamageSource damageSource -> {
				Entity entity2 = damageSource.getAttacker();
				yield entity2 == null ? value.toString() : toString(world, entity2);
			}
			case Collection<?> collection ->
					"[" + (String) collection.stream().map(v -> toString(world, v)).collect(Collectors.joining(", "))
							+ "]";
			default -> value.toString();
		};
	}

	/**
	 * Poi contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean poiContains(BlockPos pos) {
		return this.pois.contains(pos);
	}

	/**
	 * Potential poi contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean potentialPoiContains(BlockPos pos) {
		return this.potentialPois.contains(pos);
	}
}
