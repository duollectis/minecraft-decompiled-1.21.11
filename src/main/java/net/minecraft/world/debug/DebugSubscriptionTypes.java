package net.minecraft.world.debug;

import net.minecraft.entity.EntityBlockIntersectionType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.debug.data.BeeDebugData;
import net.minecraft.world.debug.data.BeeHiveDebugData;
import net.minecraft.world.debug.data.BrainDebugData;
import net.minecraft.world.debug.data.BreezeDebugData;
import net.minecraft.world.debug.data.EntityPathDebugData;
import net.minecraft.world.debug.data.GameEventDebugData;
import net.minecraft.world.debug.data.GameEventListenerDebugData;
import net.minecraft.world.debug.data.GoalSelectorDebugData;
import net.minecraft.world.debug.data.PoiDebugData;
import net.minecraft.world.debug.data.StructureDebugData;

import java.util.List;

/**
 * Реестр всех типов отладочных подписок Minecraft.
 * <p>
 * Каждая константа описывает отдельный канал отладочных данных, передаваемых
 * с сервера на клиент. Числовые значения expiry (например, 100, 200, 60) задают
 * время жизни данных в миллисекундах — после истечения клиент удаляет их из хранилища.
 */
public class DebugSubscriptionTypes<T> {

	/** Время жизни данных о пересечениях сущностей с блоками (мс). */
	private static final int ENTITY_BLOCK_INTERSECTIONS_EXPIRY = 100;

	/** Время жизни данных об ориентации красного камня (мс). */
	private static final int REDSTONE_WIRE_ORIENTATIONS_EXPIRY = 200;

	/** Время жизни данных об обновлениях соседних блоков (мс). */
	private static final int NEIGHBOR_UPDATES_EXPIRY = 200;

	/** Время жизни данных об игровых событиях (мс). */
	private static final int GAME_EVENTS_EXPIRY = 60;

	public static final DebugSubscriptionType<?> DEDICATED_SERVER_TICK_TIME =
			register("dedicated_server_tick_time");

	public static final DebugSubscriptionType<BeeDebugData> BEES =
			register("bees", BeeDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<BrainDebugData> BRAINS =
			register("brains", BrainDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<BreezeDebugData> BREEZES =
			register("breezes", BreezeDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<GoalSelectorDebugData> GOAL_SELECTORS =
			register("goal_selectors", GoalSelectorDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<EntityPathDebugData> ENTITY_PATHS =
			register("entity_paths", EntityPathDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<EntityBlockIntersectionType> ENTITY_BLOCK_INTERSECTIONS =
			register("entity_block_intersections", EntityBlockIntersectionType.PACKET_CODEC, ENTITY_BLOCK_INTERSECTIONS_EXPIRY);

	public static final DebugSubscriptionType<BeeHiveDebugData> BEE_HIVES =
			register("bee_hives", BeeHiveDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<PoiDebugData> POIS =
			register("pois", PoiDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<WireOrientation> REDSTONE_WIRE_ORIENTATIONS =
			register("redstone_wire_orientations", WireOrientation.PACKET_CODEC, REDSTONE_WIRE_ORIENTATIONS_EXPIRY);

	public static final DebugSubscriptionType<Unit> VILLAGE_SECTIONS =
			register("village_sections", Unit.PACKET_CODEC);

	public static final DebugSubscriptionType<List<BlockPos>> RAIDS =
			register("raids", BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()));

	public static final DebugSubscriptionType<List<StructureDebugData>> STRUCTURES =
			register("structures", StructureDebugData.PACKET_CODEC.collect(PacketCodecs.toList()));

	public static final DebugSubscriptionType<GameEventListenerDebugData> GAME_EVENT_LISTENERS =
			register("game_event_listeners", GameEventListenerDebugData.PACKET_CODEC);

	public static final DebugSubscriptionType<BlockPos> NEIGHBOR_UPDATES =
			register("neighbor_updates", BlockPos.PACKET_CODEC, NEIGHBOR_UPDATES_EXPIRY);

	public static final DebugSubscriptionType<GameEventDebugData> GAME_EVENTS =
			register("game_events", GameEventDebugData.PACKET_CODEC, GAME_EVENTS_EXPIRY);

	/**
	 * Инициализирует реестр и возвращает тип по умолчанию.
	 * <p>
	 * Вызывается при загрузке реестра для принудительной инициализации статических полей класса.
	 *
	 * @param registry реестр типов подписок
	 * @return тип {@link #DEDICATED_SERVER_TICK_TIME} как значение по умолчанию
	 */
	public static DebugSubscriptionType<?> registerAndGetDefault(Registry<DebugSubscriptionType<?>> registry) {
		return DEDICATED_SERVER_TICK_TIME;
	}

	private static DebugSubscriptionType<?> register(String id) {
		return Registry.register(
				Registries.DEBUG_SUBSCRIPTION,
				Identifier.ofVanilla(id),
				new DebugSubscriptionType<>(null)
		);
	}

	private static <T> DebugSubscriptionType<T> register(
			String id,
			PacketCodec<? super RegistryByteBuf, T> packetCodec
	) {
		return Registry.register(
				Registries.DEBUG_SUBSCRIPTION,
				Identifier.ofVanilla(id),
				new DebugSubscriptionType<>(packetCodec)
		);
	}

	private static <T> DebugSubscriptionType<T> register(
			String id,
			PacketCodec<? super RegistryByteBuf, T> packetCodec,
			int expiry
	) {
		return Registry.register(
				Registries.DEBUG_SUBSCRIPTION,
				Identifier.ofVanilla(id),
				new DebugSubscriptionType<>(packetCodec, expiry)
		);
	}
}
