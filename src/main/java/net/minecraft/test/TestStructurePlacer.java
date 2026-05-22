package net.minecraft.test;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Размещает структуры тестов в мире по сетке: тесты выстраиваются в ряды
 * по {@code testsPerRow} штук с отступами {@link #MARGIN_X} и {@link #MARGIN_Z} между ними.
 */
public class TestStructurePlacer implements TestRunContext.TestStructureSpawner {

	private static final int MARGIN_X = 5;
	private static final int MARGIN_Z = 6;

	private final int testsPerRow;
	private int testsInCurrentRow;
	private Box currentRowBox;
	private final BlockPos.Mutable mutablePos;
	private final BlockPos origin;
	private final boolean clearBeforeBatch;
	private float maxX = -1.0F;
	private final Collection<GameTestState> statesToClear = new ArrayList<>();

	public TestStructurePlacer(BlockPos origin, int testsPerRow, boolean clearBeforeBatch) {
		this.testsPerRow = testsPerRow;
		this.mutablePos = origin.mutableCopy();
		this.currentRowBox = new Box(mutablePos);
		this.origin = origin;
		this.clearBeforeBatch = clearBeforeBatch;
	}

	@Override
	public void onBatch(ServerWorld world) {
		if (!clearBeforeBatch) {
			return;
		}

		statesToClear.forEach(state -> {
			BlockBox blockBox = state.getTestInstanceBlockEntity().getBlockBox();
			TestInstanceUtil.clearArea(blockBox, world);
		});
		statesToClear.clear();
		currentRowBox = new Box(origin);
		mutablePos.set(origin);
	}

	@Override
	public Optional<GameTestState> spawnStructure(GameTestState gameTestState) {
		BlockPos spawnPos = new BlockPos(mutablePos);
		gameTestState.setTestBlockPos(spawnPos);

		GameTestState initialized = gameTestState.init();

		if (initialized == null) {
			return Optional.empty();
		}

		initialized.startCountdown(1);
		Box structureBox = gameTestState.getTestInstanceBlockEntity().getBox();
		currentRowBox = currentRowBox.union(structureBox);
		mutablePos.move((int) structureBox.getLengthX() + MARGIN_X, 0, 0);

		if (mutablePos.getX() > maxX) {
			maxX = mutablePos.getX();
		}

		if (++testsInCurrentRow >= testsPerRow) {
			testsInCurrentRow = 0;
			mutablePos.move(0, 0, (int) currentRowBox.getLengthZ() + MARGIN_Z);
			mutablePos.setX(origin.getX());
			currentRowBox = new Box(mutablePos);
		}

		statesToClear.add(gameTestState);
		return Optional.of(gameTestState);
	}
}
