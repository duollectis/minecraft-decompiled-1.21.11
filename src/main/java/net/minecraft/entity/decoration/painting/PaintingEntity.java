package net.minecraft.entity.decoration.painting;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Variants;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PaintingVariantTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Картина — декоративная сущность, прикреплённая к стене.
 * Вариант картины определяет её размер и текстуру.
 * При размещении выбирается наибольший подходящий вариант из тега {@code PLACEABLE}.
 */
public class PaintingEntity extends AbstractDecorationEntity {

	private static final TrackedData<RegistryEntry<PaintingVariant>> VARIANT = DataTracker.registerData(
			PaintingEntity.class, TrackedDataHandlerRegistry.PAINTING_VARIANT
	);

	/** Размер одного пикселя картины в блоках (1/16 блока). */
	public static final float PIXEL_SIZE = 0.0625F;

	/** Смещение плоскости картины от поверхности блока (15/32 блока). */
	private static final double PAINTING_DEPTH_OFFSET = 0.46875;

	public PaintingEntity(EntityType<? extends PaintingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, Variants.getDefaultOrThrow(getRegistryManager(), RegistryKeys.PAINTING_VARIANT));
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (VARIANT.equals(data)) {
			updateAttachmentPosition();
		}
	}

	private void setVariant(RegistryEntry<PaintingVariant> variant) {
		dataTracker.set(VARIANT, variant);
	}

	public RegistryEntry<PaintingVariant> getVariant() {
		return dataTracker.get(VARIANT);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.PAINTING_VARIANT
				? castComponentValue((ComponentType<T>) type, getVariant())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.PAINTING_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.PAINTING_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.PAINTING_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	/**
	 * Выбирает наибольший подходящий вариант картины из тега PLACEABLE и размещает её.
	 * Если несколько вариантов имеют одинаковую максимальную площадь — выбирается случайный.
	 *
	 * @return картина с выбранным вариантом, или {@link Optional#empty()} если нет подходящих
	 */
	public static Optional<PaintingEntity> placePainting(World world, BlockPos pos, Direction facing) {
		PaintingEntity painting = new PaintingEntity(world, pos);
		List<RegistryEntry<PaintingVariant>> candidates = new ArrayList<>();
		world.getRegistryManager()
				.getOrThrow(RegistryKeys.PAINTING_VARIANT)
				.iterateEntries(PaintingVariantTags.PLACEABLE)
				.forEach(candidates::add);

		if (candidates.isEmpty()) {
			return Optional.empty();
		}

		painting.setFacing(facing);
		candidates.removeIf(variant -> {
			painting.setVariant(variant);
			return !painting.canStayAttached();
		});

		if (candidates.isEmpty()) {
			return Optional.empty();
		}

		int maxSize = candidates.stream().mapToInt(PaintingEntity::getVariantArea).max().orElse(0);
		candidates.removeIf(variant -> getVariantArea(variant) < maxSize);
		Optional<RegistryEntry<PaintingVariant>> chosen = Util.getRandomOrEmpty(candidates, painting.random);
		if (chosen.isEmpty()) {
			return Optional.empty();
		}

		painting.setVariant(chosen.get());
		painting.setFacing(facing);
		return Optional.of(painting);
	}

	private static int getVariantArea(RegistryEntry<PaintingVariant> variant) {
		return variant.value().getArea();
	}

	private PaintingEntity(World world, BlockPos pos) {
		super(EntityType.PAINTING, world, pos);
	}

	public PaintingEntity(World world, BlockPos pos, Direction direction, RegistryEntry<PaintingVariant> variant) {
		this(world, pos);
		setVariant(variant);
		setFacing(direction);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("facing", Direction.HORIZONTAL_QUARTER_TURNS_CODEC, getHorizontalFacing());
		super.writeCustomData(view);
		Variants.writeData(view, getVariant());
	}

	@Override
	protected void readCustomData(ReadView view) {
		Direction direction = view.<Direction>read("facing", Direction.HORIZONTAL_QUARTER_TURNS_CODEC)
				.orElse(Direction.SOUTH);
		super.readCustomData(view);
		setFacing(direction);
		Variants.fromData(view, RegistryKeys.PAINTING_VARIANT).ifPresent(this::setVariant);
	}

	/**
	 * Вычисляет AABB картины. Толщина по оси прикрепления — {@value #PIXEL_SIZE} блока.
	 * Смещение центра учитывает чётность размеров варианта.
	 */
	@Override
	protected Box calculateBoundingBox(BlockPos pos, Direction side) {
		Vec3d center = Vec3d.ofCenter(pos).offset(side, -PAINTING_DEPTH_OFFSET);
		PaintingVariant variant = getVariant().value();
		double offsetX = getOffset(variant.width());
		double offsetY = getOffset(variant.height());
		Direction perpendicular = side.rotateYCounterclockwise();
		Vec3d origin = center.offset(perpendicular, offsetX).offset(Direction.UP, offsetY);
		Direction.Axis axis = side.getAxis();
		double sizeX = axis == Direction.Axis.X ? PIXEL_SIZE : variant.width();
		double sizeY = variant.height();
		double sizeZ = axis == Direction.Axis.Z ? PIXEL_SIZE : variant.width();
		return Box.of(origin, sizeX, sizeY, sizeZ);
	}

	/** Возвращает смещение центра для выравнивания картины по сетке блоков. */
	private double getOffset(int length) {
		return length % 2 == 0 ? 0.5 : 0.0;
	}

	@Override
	public void onBreak(ServerWorld world, @Nullable Entity breaker) {
		if (!world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			return;
		}

		playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
		if (!(breaker instanceof PlayerEntity playerEntity && playerEntity.isInCreativeMode())) {
			dropItem(world, Items.PAINTING);
		}
	}

	@Override
	public void onPlace() {
		playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
	}

	@Override
	public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
		setPosition(x, y, z);
	}

	@Override
	public Vec3d getSyncedPos() {
		return Vec3d.of(attachedBlockPos);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, getHorizontalFacing().getIndex(), getAttachedBlockPos());
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		setFacing(Direction.byIndex(packet.getEntityData()));
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.PAINTING);
	}
}
