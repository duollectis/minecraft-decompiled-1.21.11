package net.minecraft.item.map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapColorComponent;
import net.minecraft.component.type.MapDecorationsComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;

/**
 * Хранит состояние заполненной карты: цвета пикселей, декорации (баннеры, рамки, игроки),
 * масштаб, центр и измерение. Синхронизируется с клиентами через {@link MapUpdateS2CPacket}.
 */
public class MapState extends PersistentState {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int SIZE = 128;
	private static final int SIZE_HALF = 64;
	public static final int MAX_SCALE = 4;
	public static final int MAX_DECORATIONS = 256;
	private static final String FRAME_PREFIX = "frame-";
	private static final int DECORATION_SYNC_INTERVAL = 5;
	public static final Codec<MapState> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    World.CODEC.fieldOf("dimension").forGetter(mapState -> mapState.dimension),
					                    Codec.INT.fieldOf("xCenter").forGetter(mapState -> mapState.centerX),
					                    Codec.INT.fieldOf("zCenter").forGetter(mapState -> mapState.centerZ),
					                    Codec.BYTE.optionalFieldOf("scale", (byte) 0).forGetter(mapState -> mapState.scale),
					                    Codec.BYTE_BUFFER.fieldOf("colors").forGetter(mapState -> ByteBuffer.wrap(mapState.colors)),
					                    Codec.BOOL
							                    .optionalFieldOf("trackingPosition", true)
							                    .forGetter(mapState -> mapState.showDecorations),
					                    Codec.BOOL
							                    .optionalFieldOf("unlimitedTracking", false)
							                    .forGetter(mapState -> mapState.unlimitedTracking),
					                    Codec.BOOL.optionalFieldOf("locked", false).forGetter(mapState -> mapState.locked),
					                    MapBannerMarker.CODEC
							                    .listOf()
							                    .optionalFieldOf("banners", List.of())
							                    .forGetter(mapState -> List.copyOf(mapState.banners.values())),
					                    MapFrameMarker.CODEC
							                    .listOf()
							                    .optionalFieldOf("frames", List.of())
							                    .forGetter(mapState -> List.copyOf(mapState.frames.values()))
			                    )
			                    .apply(instance, MapState::new)
	);
	public final int centerX;
	public final int centerZ;
	public final RegistryKey<World> dimension;
	private final boolean showDecorations;
	private final boolean unlimitedTracking;
	public final byte scale;
	public byte[] colors = new byte[SIZE * SIZE];
	public final boolean locked;
	private final List<MapState.PlayerUpdateTracker> updateTrackers = Lists.newArrayList();
	private final Map<PlayerEntity, MapState.PlayerUpdateTracker> updateTrackersByPlayer = Maps.newHashMap();
	private final Map<String, MapBannerMarker> banners = Maps.newHashMap();
	final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
	private final Map<String, MapFrameMarker> frames = Maps.newHashMap();
	private int decorationCount;

	/**
	 * Создаёт state type.
	 *
	 * @param mapId map id
	 *
	 * @return PersistentStateType — результат операции
	 */
	public static PersistentStateType<MapState> createStateType(MapIdComponent mapId) {
		return new PersistentStateType<>(
				mapId.asString(), () -> {
			throw new IllegalStateException("Should never create an empty map saved data");
		}, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA
		);
	}

	private MapState(
			int centerX,
			int centerZ,
			byte scale,
			boolean showDecorations,
			boolean unlimitedTracking,
			boolean locked,
			RegistryKey<World> dimension
	) {
		this.scale = scale;
		this.centerX = centerX;
		this.centerZ = centerZ;
		this.dimension = dimension;
		this.showDecorations = showDecorations;
		this.unlimitedTracking = unlimitedTracking;
		this.locked = locked;
	}

	private MapState(
			RegistryKey<World> dimension,
			int centerX,
			int centerZ,
			byte scale,
			ByteBuffer colors,
			boolean showDecorations,
			boolean unlimitedTracking,
			boolean locked,
			List<MapBannerMarker> banners,
			List<MapFrameMarker> frames
	) {
		this(
				centerX,
				centerZ,
				(byte) MathHelper.clamp(scale, 0, 4),
				showDecorations,
				unlimitedTracking,
				locked,
				dimension
		);
		if (colors.array().length == SIZE * SIZE) {
			this.colors = colors.array();
		}

		for (MapBannerMarker mapBannerMarker : banners) {
			this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
			this.addDecoration(
					mapBannerMarker.getDecorationType(),
					null,
					mapBannerMarker.getKey(),
					mapBannerMarker.pos().getX(),
					mapBannerMarker.pos().getZ(),
					180.0,
					mapBannerMarker.name().orElse(null)
			);
		}

		for (MapFrameMarker mapFrameMarker : frames) {
			this.frames.put(mapFrameMarker.getKey(), mapFrameMarker);
			this.addDecoration(
					MapDecorationTypes.FRAME,
					null,
					getFrameDecorationKey(mapFrameMarker.entityId()),
					mapFrameMarker.pos().getX(),
					mapFrameMarker.pos().getZ(),
					mapFrameMarker.rotation(),
					null
			);
		}
	}

	public static MapState of(
			double centerX,
			double centerZ,
			byte scale,
			boolean showDecorations,
			boolean unlimitedTracking,
			RegistryKey<World> dimension
	) {
		int blockSize = SIZE * (1 << scale);
		int gridX = MathHelper.floor((centerX + 64.0) / blockSize);
		int gridZ = MathHelper.floor((centerZ + 64.0) / blockSize);
		int snappedCenterX = gridX * blockSize + blockSize / 2 - SIZE_HALF;
		int snappedCenterZ = gridZ * blockSize + blockSize / 2 - SIZE_HALF;
		return new MapState(snappedCenterX, snappedCenterZ, scale, showDecorations, unlimitedTracking, false, dimension);
	}

	/**
	 * Of.
	 *
	 * @param scale scale
	 * @param locked locked
	 * @param dimension dimension
	 *
	 * @return MapState — результат операции
	 */
	public static MapState of(byte scale, boolean locked, RegistryKey<World> dimension) {
		return new MapState(0, 0, scale, false, false, locked, dimension);
	}

	/**
	 * Copy.
	 *
	 * @return MapState — результат операции
	 */
	public MapState copy() {
		MapState
				mapState =
				new MapState(
						this.centerX,
						this.centerZ,
						this.scale,
						this.showDecorations,
						this.unlimitedTracking,
						true,
						this.dimension
				);
		mapState.banners.putAll(this.banners);
		mapState.decorations.putAll(this.decorations);
		mapState.decorationCount = this.decorationCount;
		System.arraycopy(this.colors, 0, mapState.colors, 0, this.colors.length);
		return mapState;
	}

	/**
	 * Zoom out.
	 *
	 * @return MapState — результат операции
	 */
	public MapState zoomOut() {
		return of(
				this.centerX,
				this.centerZ,
				(byte) MathHelper.clamp(this.scale + 1, 0, 4),
				this.showDecorations,
				this.unlimitedTracking,
				this.dimension
		);
	}

	private static Predicate<ItemStack> getEqualPredicate(ItemStack stack) {
		MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
		return other -> other == stack ? true : other.isOf(stack.getItem()) && Objects.equals(
				mapIdComponent,
				other.get(DataComponentTypes.MAP_ID)
		);
	}

	/**
	 * Update.
	 *
	 * @param player player
	 * @param stack stack
	 */
	public void update(PlayerEntity player, ItemStack stack) {
		if (!this.updateTrackersByPlayer.containsKey(player)) {
			MapState.PlayerUpdateTracker playerUpdateTracker = new MapState.PlayerUpdateTracker(player);
			this.updateTrackersByPlayer.put(player, playerUpdateTracker);
			this.updateTrackers.add(playerUpdateTracker);
		}

		Predicate<ItemStack> predicate = getEqualPredicate(stack);
		if (!player.getInventory().contains(predicate)) {
			this.removeDecoration(player.getStringifiedName());
		}

		for (int i = 0; i < this.updateTrackers.size(); i++) {
			MapState.PlayerUpdateTracker playerUpdateTracker2 = this.updateTrackers.get(i);
			PlayerEntity playerEntity = playerUpdateTracker2.player;
			String string = playerEntity.getStringifiedName();
			if (!playerEntity.isRemoved() && (playerEntity.getInventory().contains(predicate) || stack.isInFrame())) {
				if (!stack.isInFrame() && playerEntity.getEntityWorld().getRegistryKey() == this.dimension
						&& this.showDecorations) {
					this.addDecoration(
							MapDecorationTypes.PLAYER,
							playerEntity.getEntityWorld(),
							string,
							playerEntity.getX(),
							playerEntity.getZ(),
							playerEntity.getYaw(),
							null
					);
				}
			}
			else {
				this.updateTrackersByPlayer.remove(playerEntity);
				this.updateTrackers.remove(playerUpdateTracker2);
				this.removeDecoration(string);
			}

			if (!playerEntity.equals(player) && hasMapInvisibilityEquipment(playerEntity)) {
				this.removeDecoration(string);
			}
		}

		if (stack.isInFrame() && this.showDecorations) {
			ItemFrameEntity itemFrameEntity = stack.getFrame();
			BlockPos blockPos = itemFrameEntity.getAttachedBlockPos();
			MapFrameMarker mapFrameMarker = this.frames.get(MapFrameMarker.getKey(blockPos));
			if (mapFrameMarker != null && itemFrameEntity.getId() != mapFrameMarker.entityId()
					&& this.frames.containsKey(mapFrameMarker.getKey())) {
				this.removeDecoration(getFrameDecorationKey(mapFrameMarker.entityId()));
			}

			MapFrameMarker mapFrameMarker2 = new MapFrameMarker(
					blockPos,
					itemFrameEntity.getHorizontalFacing().getHorizontalQuarterTurns() * 90,
					itemFrameEntity.getId()
			);
			this.addDecoration(
					MapDecorationTypes.FRAME,
					player.getEntityWorld(),
					getFrameDecorationKey(itemFrameEntity.getId()),
					blockPos.getX(),
					blockPos.getZ(),
					itemFrameEntity.getHorizontalFacing().getHorizontalQuarterTurns() * 90,
					null
			);
			MapFrameMarker mapFrameMarker3 = this.frames.put(mapFrameMarker2.getKey(), mapFrameMarker2);
			if (!mapFrameMarker2.equals(mapFrameMarker3)) {
				this.markDirty();
			}
		}

		MapDecorationsComponent
				mapDecorationsComponent =
				stack.getOrDefault(DataComponentTypes.MAP_DECORATIONS, MapDecorationsComponent.DEFAULT);
		if (!this.decorations.keySet().containsAll(mapDecorationsComponent.decorations().keySet())) {
			mapDecorationsComponent.decorations().forEach((id, decoration) -> {
				if (!this.decorations.containsKey(id)) {
					this.addDecoration(
							decoration.type(),
							player.getEntityWorld(),
							id,
							decoration.x(),
							decoration.z(),
							decoration.rotation(),
							null
					);
				}
			});
		}
	}

	private static boolean hasMapInvisibilityEquipment(PlayerEntity player) {
		for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
			if (equipmentSlot != EquipmentSlot.MAINHAND
					&& equipmentSlot != EquipmentSlot.OFFHAND
					&& player.getEquippedStack(equipmentSlot).isIn(ItemTags.MAP_INVISIBILITY_EQUIPMENT)) {
				return true;
			}
		}

		return false;
	}

	private void removeDecoration(String id) {
		MapDecoration mapDecoration = this.decorations.remove(id);
		if (mapDecoration != null && mapDecoration.type().value().trackCount()) {
			this.decorationCount--;
		}

		this.markDecorationsDirty();
	}

	public static void addDecorationsNbt(
			ItemStack stack,
			BlockPos pos,
			String id,
			RegistryEntry<MapDecorationType> decorationType
	) {
		MapDecorationsComponent.Decoration
				decoration =
				new MapDecorationsComponent.Decoration(decorationType, pos.getX(), pos.getZ(), 180.0F);
		stack.apply(
				DataComponentTypes.MAP_DECORATIONS,
				MapDecorationsComponent.DEFAULT,
				decorations -> decorations.with(id, decoration)
		);
		if (decorationType.value().hasMapColor()) {
			stack.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(decorationType.value().mapColor()));
		}
	}

	private void addDecoration(
			RegistryEntry<MapDecorationType> type,
			@Nullable WorldAccess world,
			String key,
			double x,
			double z,
			double rotation,
			@Nullable Text text
	) {
		int scaleFactor = 1 << this.scale;
		float dx = (float) (x - this.centerX) / scaleFactor;
		float dz = (float) (z - this.centerZ) / scaleFactor;
		MapState.Marker marker = this.getMarker(type, world, rotation, dx, dz);

		if (marker == null) {
			this.removeDecoration(key);
			return;
		}

		MapDecoration decoration = new MapDecoration(marker.type(), marker.x(), marker.y(), marker.rot(), Optional.ofNullable(text));
		MapDecoration previous = this.decorations.put(key, decoration);

		if (decoration.equals(previous)) {
			return;
		}

		if (previous != null && previous.type().value().trackCount()) {
			this.decorationCount--;
		}

		if (marker.type().value().trackCount()) {
			this.decorationCount++;
		}

		this.markDecorationsDirty();
	}

	private MapState.@Nullable Marker getMarker(
			RegistryEntry<MapDecorationType> type,
			@Nullable WorldAccess world,
			double rotation,
			float dx,
			float dz
	) {
		byte markerX = offsetToMarkerPosition(dx);
		byte markerZ = offsetToMarkerPosition(dz);

		if (type.matches(MapDecorationTypes.PLAYER)) {
			Pair<RegistryEntry<MapDecorationType>, Byte> pair = this.getPlayerMarkerAndRotation(type, world, rotation, dx, dz);
			return pair == null
					? null
					: new MapState.Marker(pair.getFirst(), markerX, markerZ, pair.getSecond());
		}

		return (isInBounds(dx, dz) || this.unlimitedTracking)
				? new MapState.Marker(type, markerX, markerZ, this.getPlayerMarkerRotation(world, rotation))
				: null;
	}

	private @Nullable Pair<RegistryEntry<MapDecorationType>, Byte> getPlayerMarkerAndRotation(
			RegistryEntry<MapDecorationType> type, @Nullable WorldAccess world, double rotation, float dx, float dz
	) {
		if (isInBounds(dx, dz)) {
			return Pair.of(type, this.getPlayerMarkerRotation(world, rotation));
		}

		RegistryEntry<MapDecorationType> offMapMarker = this.getPlayerMarker(dx, dz);
		return offMapMarker == null ? null : Pair.of(offMapMarker, (byte) 0);
	}

	private byte getPlayerMarkerRotation(@Nullable WorldAccess world, double rotation) {
		if (this.dimension == World.NETHER && world != null) {
			// В Нижнем мире стрелка вращается псевдослучайно на основе игрового времени
			int tick = (int) (world.getTime() / NETHER_ROTATION_TICK_DIVISOR);
			return (byte) (tick * tick * NETHER_ROTATION_MULTIPLIER_A + tick * NETHER_ROTATION_MULTIPLIER_B >> NETHER_ROTATION_SHIFT & NETHER_ROTATION_MASK);
		}

		double adjusted = rotation < 0.0 ? rotation - 8.0 : rotation + 8.0;
		return (byte) (adjusted * 16.0 / 360.0);
	}

	private static final long NETHER_ROTATION_TICK_DIVISOR = 10L;
	private static final int NETHER_ROTATION_MULTIPLIER_A = 34187121;
	private static final int NETHER_ROTATION_MULTIPLIER_B = 121;
	private static final int NETHER_ROTATION_SHIFT = 15;
	private static final int NETHER_ROTATION_MASK = 15;
	private static final float MAP_HALF_SIZE = 63.0F;
	private static final float PLAYER_OFF_MAP_RADIUS = 320.0F;
	private static final byte MARKER_MAX = 127;

	private static boolean isInBounds(float dx, float dz) {
		return dx >= -MAP_HALF_SIZE && dz >= -MAP_HALF_SIZE && dx <= MAP_HALF_SIZE && dz <= MAP_HALF_SIZE;
	}

	private @Nullable RegistryEntry<MapDecorationType> getPlayerMarker(float dx, float dz) {
		boolean isNearby = Math.abs(dx) < PLAYER_OFF_MAP_RADIUS && Math.abs(dz) < PLAYER_OFF_MAP_RADIUS;
		if (isNearby) {
			return MapDecorationTypes.PLAYER_OFF_MAP;
		}

		return this.unlimitedTracking ? MapDecorationTypes.PLAYER_OFF_LIMITS : null;
	}

	private static byte offsetToMarkerPosition(float offset) {
		if (offset <= -MAP_HALF_SIZE) {
			return -SIZE;
		}

		return offset >= MAP_HALF_SIZE ? MARKER_MAX : (byte) (offset * 2.0F + 0.5);
	}

	public @Nullable Packet<?> getPlayerMarkerPacket(MapIdComponent mapId, PlayerEntity player) {
		MapState.PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
		return playerUpdateTracker == null ? null : playerUpdateTracker.getPacket(mapId);
	}

	private void markDirty(int x, int z) {
		this.markDirty();

		for (MapState.PlayerUpdateTracker playerUpdateTracker : this.updateTrackers) {
			playerUpdateTracker.markDirty(x, z);
		}
	}

	private void markDecorationsDirty() {
		this.updateTrackers.forEach(MapState.PlayerUpdateTracker::markDecorationsDirty);
	}

	public MapState.PlayerUpdateTracker getPlayerSyncData(PlayerEntity player) {
		MapState.PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
		if (playerUpdateTracker == null) {
			playerUpdateTracker = new MapState.PlayerUpdateTracker(player);
			this.updateTrackersByPlayer.put(player, playerUpdateTracker);
			this.updateTrackers.add(playerUpdateTracker);
		}

		return playerUpdateTracker;
	}

	/**
	 * Добавляет banner.
	 *
	 * @param world world
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean addBanner(WorldAccess world, BlockPos pos) {
		double bannerX = pos.getX() + 0.5;
		double bannerZ = pos.getZ() + 0.5;
		int scaleFactor = 1 << this.scale;
		double relativeX = (bannerX - this.centerX) / scaleFactor;
		double relativeZ = (bannerZ - this.centerZ) / scaleFactor;

		if (relativeX >= -MAP_HALF_SIZE && relativeZ >= -MAP_HALF_SIZE && relativeX <= MAP_HALF_SIZE && relativeZ <= MAP_HALF_SIZE) {
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
			if (mapBannerMarker == null) {
				return false;
			}

			if (this.banners.remove(mapBannerMarker.getKey(), mapBannerMarker)) {
				this.removeDecoration(mapBannerMarker.getKey());
				this.markDirty();
				return true;
			}

			if (!this.decorationCountNotLessThan(MAX_DECORATIONS)) {
				this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
				this.addDecoration(
						mapBannerMarker.getDecorationType(),
						world,
						mapBannerMarker.getKey(),
						bannerX,
						bannerZ,
						180.0,
						mapBannerMarker.name().orElse(null)
				);
				this.markDirty();
				return true;
			}
		}

		return false;
	}

	/**
	 * Удаляет banner.
	 *
	 * @param world world
	 * @param x x
	 * @param z z
	 */
	public void removeBanner(BlockView world, int x, int z) {
		Iterator<MapBannerMarker> iterator = this.banners.values().iterator();

		while (iterator.hasNext()) {
			MapBannerMarker mapBannerMarker = iterator.next();
			if (mapBannerMarker.pos().getX() == x && mapBannerMarker.pos().getZ() == z) {
				MapBannerMarker mapBannerMarker2 = MapBannerMarker.fromWorldBlock(world, mapBannerMarker.pos());
				if (!mapBannerMarker.equals(mapBannerMarker2)) {
					iterator.remove();
					this.removeDecoration(mapBannerMarker.getKey());
					this.markDirty();
				}
			}
		}
	}

	public Collection<MapBannerMarker> getBanners() {
		return this.banners.values();
	}

	/**
	 * Удаляет frame.
	 *
	 * @param pos pos
	 * @param id id
	 */
	public void removeFrame(BlockPos pos, int id) {
		this.removeDecoration(getFrameDecorationKey(id));
		this.frames.remove(MapFrameMarker.getKey(pos));
		this.markDirty();
	}

	/**
	 * Put color.
	 *
	 * @param x x
	 * @param z z
	 * @param color color
	 *
	 * @return boolean — результат операции
	 */
	public boolean putColor(int x, int z, byte color) {
		byte existing = this.colors[x + z * SIZE];
		if (existing == color) {
			return false;
		}

		this.setColor(x, z, color);
		return true;
	}

	public void setColor(int x, int z, byte color) {
		this.colors[x + z * SIZE] = color;
		this.markDirty(x, z);
	}

	public boolean hasExplorationMapDecoration() {
		for (MapDecoration mapDecoration : this.decorations.values()) {
			if (mapDecoration.type().value().explorationMapElement()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Replace decorations.
	 *
	 * @param decorations decorations
	 */
	public void replaceDecorations(List<MapDecoration> decorations) {
		this.decorations.clear();
		this.decorationCount = 0;

		for (int i = 0; i < decorations.size(); i++) {
			MapDecoration mapDecoration = decorations.get(i);
			this.decorations.put("icon-" + i, mapDecoration);
			if (mapDecoration.type().value().trackCount()) {
				this.decorationCount++;
			}
		}
	}

	public Iterable<MapDecoration> getDecorations() {
		return this.decorations.values();
	}

	/**
	 * Decoration count not less than.
	 *
	 * @param decorationCount decoration count
	 *
	 * @return boolean — результат операции
	 */
	public boolean decorationCountNotLessThan(int decorationCount) {
		return this.decorationCount >= decorationCount;
	}

	private static String getFrameDecorationKey(int id) {
		return FRAME_PREFIX + id;
	}

	/**
	 * Временный маркер игрока или сущности на карте, вычисляемый за один тик.
	 * Хранит тип декорации, позицию в координатах карты (−128..127) и угол поворота,
	 * после чего передаётся в {@link MapState#addDecoration} для обновления отображения.
	 */
	record Marker(RegistryEntry<MapDecorationType> type, byte x, byte y, byte rot) {
	}

	/**
	 * {@code PlayerUpdateTracker}.
	 */
	public class PlayerUpdateTracker {

		public final PlayerEntity player;
		private boolean dirty = true;
		private int startX;
		private int startZ;
		private int endX = SIZE - 1;
		private int endZ = SIZE - 1;
		private boolean decorationsDirty = true;
		private int emptyPacketsRequested;
		public int updateTick;

		PlayerUpdateTracker(final PlayerEntity player) {
			this.player = player;
		}

		private MapState.UpdateData getMapUpdateData() {
			int width = this.endX + 1 - this.startX;
			int height = this.endZ + 1 - this.startZ;
			byte[] patch = new byte[width * height];

			for (int col = 0; col < width; col++) {
				for (int row = 0; row < height; row++) {
					patch[col + row * width] = MapState.this.colors[this.startX + col + (this.startZ + row) * SIZE];
				}
			}

			return new MapState.UpdateData(this.startX, this.startZ, width, height, patch);
		}

		@Nullable Packet<?> getPacket(MapIdComponent mapId) {
			MapState.UpdateData updateData = null;
			if (this.dirty) {
				this.dirty = false;
				updateData = this.getMapUpdateData();
			}

			Collection<MapDecoration> decorations = null;
			if (this.decorationsDirty && this.emptyPacketsRequested++ % DECORATION_SYNC_INTERVAL == 0) {
				this.decorationsDirty = false;
				decorations = MapState.this.decorations.values();
			}

			return (decorations == null && updateData == null)
					? null
					: new MapUpdateS2CPacket(mapId, MapState.this.scale, MapState.this.locked, decorations, updateData);
		}

		void markDirty(int startX, int startZ) {
			if (this.dirty) {
				this.startX = Math.min(this.startX, startX);
				this.startZ = Math.min(this.startZ, startZ);
				this.endX = Math.max(this.endX, startX);
				this.endZ = Math.max(this.endZ, startZ);
			}
			else {
				this.dirty = true;
				this.startX = startX;
				this.startZ = startZ;
				this.endX = startX;
				this.endZ = startZ;
			}
		}

		private void markDecorationsDirty() {
			this.decorationsDirty = true;
		}
	}

	/**
	 * Дельта-обновление пикселей карты, отправляемое клиенту по сети.
	 * Описывает прямоугольный патч изменившихся цветов с координатами начала
	 * и размерами, что позволяет передавать только изменённую область вместо всей карты.
	 */
	public record UpdateData(int startX, int startZ, int width, int height, byte[] colors) {

		public static final PacketCodec<ByteBuf, Optional<MapState.UpdateData>> CODEC = PacketCodec.ofStatic(
				MapState.UpdateData::encode, MapState.UpdateData::decode
		);

		private static void encode(ByteBuf buf, Optional<MapState.UpdateData> updateData) {
			if (updateData.isPresent()) {
				MapState.UpdateData updateData2 = updateData.get();
				buf.writeByte(updateData2.width);
				buf.writeByte(updateData2.height);
				buf.writeByte(updateData2.startX);
				buf.writeByte(updateData2.startZ);
				PacketByteBuf.writeByteArray(buf, updateData2.colors);
			}
			else {
				buf.writeByte(0);
			}
		}

		private static Optional<MapState.UpdateData> decode(ByteBuf buf) {
			int width = buf.readUnsignedByte();
			if (width == 0) {
				return Optional.empty();
			}

			int height = buf.readUnsignedByte();
			int startX = buf.readUnsignedByte();
			int startZ = buf.readUnsignedByte();
			byte[] colors = PacketByteBuf.readByteArray(buf);
			return Optional.of(new MapState.UpdateData(startX, startZ, width, height, colors));
		}

		public void setColorsTo(MapState mapState) {
			for (int col = 0; col < this.width; col++) {
				for (int row = 0; row < this.height; row++) {
					mapState.setColor(this.startX + col, this.startZ + row, this.colors[col + row * this.width]);
				}
			}
		}
	}
}
