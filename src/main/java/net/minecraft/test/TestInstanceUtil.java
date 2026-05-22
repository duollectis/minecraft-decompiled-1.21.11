package net.minecraft.test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Утилитарный класс для работы с тестовыми блоками и структурами в мире.
 */
public class TestInstanceUtil {

	public static final int DEFAULT_TIMEOUT_TICKS = 10;
	public static final String TEST_STRUCTURES_DIRECTORY_NAME = "Minecraft.Server/src/test/convertables/data";
	public static Path testStructuresDirectoryName = Paths.get("Minecraft.Server/src/test/convertables/data");

	/**
	 * Преобразует количество шагов поворота (0–3) в {@link BlockRotation}.
	 *
	 * @param steps количество шагов по часовой стрелке (0 = нет, 1 = 90°, 2 = 180°, 3 = 270°)
	 * @return соответствующий {@link BlockRotation}
	 * @throws IllegalArgumentException если steps не в диапазоне 0–3
	 */
	public static BlockRotation getRotation(int steps) {
		return switch (steps) {
			case 0 -> BlockRotation.NONE;
			case 1 -> BlockRotation.CLOCKWISE_90;
			case 2 -> BlockRotation.CLOCKWISE_180;
			case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
			default -> throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + steps);
		};
	}

	/**
	 * Преобразует {@link BlockRotation} в количество шагов поворота (0–3).
	 *
	 * @param rotation поворот блока
	 * @return количество шагов (0–3)
	 * @throws IllegalArgumentException если передан неизвестный поворот
	 */
	public static int getRotationSteps(BlockRotation rotation) {
		return switch (rotation) {
			case NONE -> 0;
			case CLOCKWISE_90 -> 1;
			case CLOCKWISE_180 -> 2;
			case COUNTERCLOCKWISE_90 -> 3;
			default -> throw new IllegalArgumentException(
				"Unknown rotation value, don't know how many steps it represents: " + rotation
			);
		};
	}

	public static TestInstanceBlockEntity createTestInstanceBlockEntity(
		Identifier testInstanceId,
		BlockPos pos,
		Vec3i size,
		BlockRotation rotation,
		ServerWorld world
	) {
		BlockBox blockBox = getTestInstanceBlockBox(TestInstanceBlockEntity.getStructurePos(pos), size, rotation);
		clearArea(blockBox, world);
		world.setBlockState(pos, Blocks.TEST_INSTANCE_BLOCK.getDefaultState());

		TestInstanceBlockEntity blockEntity = (TestInstanceBlockEntity) world.getBlockEntity(pos);
		RegistryKey<TestInstance> registryKey = RegistryKey.of(RegistryKeys.TEST_INSTANCE, testInstanceId);
		blockEntity.setData(
			new TestInstanceBlockEntity.Data(
				Optional.of(registryKey),
				size,
				rotation,
				false,
				TestInstanceBlockEntity.Status.CLEARED,
				Optional.empty()
			)
		);

		return blockEntity;
	}

	public static void clearArea(BlockBox area, ServerWorld world) {
		int floorY = area.getMinY() - 1;
		BlockPos.stream(area).forEach(pos -> resetBlock(floorY, pos, world));
		world.getBlockTickScheduler().clearNextTicks(area);
		world.clearUpdatesInArea(area);

		Box box = Box.from(area);
		List<Entity> entities = world.getEntitiesByClass(
			Entity.class,
			box,
			entity -> !(entity instanceof PlayerEntity)
		);
		entities.forEach(Entity::discard);
	}

	public static BlockPos getTestInstanceBlockBoxCornerPos(BlockPos pos, Vec3i size, BlockRotation rotation) {
		BlockPos corner = pos.add(size).add(-1, -1, -1);
		return StructureTemplate.transformAround(corner, BlockMirror.NONE, rotation, pos);
	}

	public static BlockBox getTestInstanceBlockBox(BlockPos pos, Vec3i relativePos, BlockRotation rotation) {
		BlockPos corner = getTestInstanceBlockBoxCornerPos(pos, relativePos, rotation);
		BlockBox blockBox = BlockBox.create(pos, corner);
		int minX = Math.min(blockBox.getMinX(), blockBox.getMaxX());
		int minZ = Math.min(blockBox.getMinZ(), blockBox.getMaxZ());
		return blockBox.move(pos.getX() - minX, 0, pos.getZ() - minZ);
	}

	public static Optional<BlockPos> findContainingTestInstanceBlock(BlockPos pos, int radius, ServerWorld world) {
		return findTestInstanceBlocks(pos, radius, world)
			.filter(blockPos -> isInBounds(blockPos, pos, world))
			.findFirst();
	}

	public static Optional<BlockPos> findNearestTestInstanceBlock(BlockPos pos, int radius, ServerWorld world) {
		Comparator<BlockPos> byDistance = Comparator.comparingInt(blockPos -> blockPos.getManhattanDistance(pos));
		return findTestInstanceBlocks(pos, radius, world).min(byDistance);
	}

	public static Stream<BlockPos> findTestInstanceBlocks(BlockPos pos, int radius, ServerWorld world) {
		return world.getPointOfInterestStorage()
			.getPositions(
				poiType -> poiType.matchesKey(PointOfInterestTypes.TEST_INSTANCE),
				poiPos -> true,
				pos,
				radius,
				PointOfInterestStorage.OccupationStatus.ANY
			)
			.map(BlockPos::toImmutable);
	}

	/**
	 * Находит тестовый блок, на который смотрит сущность, в радиусе 250 блоков.
	 * Использует рейкаст от позиции глаз в направлении взгляда.
	 */
	public static Stream<BlockPos> findTargetedTestInstanceBlock(BlockPos pos, Entity entity, ServerWorld world) {
		int searchRadius = 250;
		Vec3d eyePos = entity.getEyePos();
		Vec3d lookTarget = eyePos.add(entity.getRotationVector().multiply(searchRadius));

		return findTestInstanceBlocks(pos, searchRadius, world)
			.map(blockPos -> world.getBlockEntity(blockPos, BlockEntityType.TEST_INSTANCE_BLOCK))
			.flatMap(Optional::stream)
			.filter(testBlockEntity -> testBlockEntity.getBox().raycast(eyePos, lookTarget).isPresent())
			.map(BlockEntity::getPos)
			.sorted(Comparator.comparing(pos::getSquaredDistance))
			.limit(1L);
	}

	private static void resetBlock(int altitude, BlockPos pos, ServerWorld world) {
		BlockState state = pos.getY() < altitude
			? Blocks.STONE.getDefaultState()
			: Blocks.AIR.getDefaultState();

		BlockStateArgument blockStateArgument = new BlockStateArgument(state, Collections.emptySet(), null);
		blockStateArgument.setBlockState(world, pos, 818);
		world.updateNeighbors(pos, state.getBlock());
	}

	private static boolean isInBounds(BlockPos testInstanceBlockPos, BlockPos pos, ServerWorld world) {
		return world.getBlockEntity(testInstanceBlockPos) instanceof TestInstanceBlockEntity blockEntity
			? blockEntity.getBlockBox().contains(pos)
			: false;
	}
}
