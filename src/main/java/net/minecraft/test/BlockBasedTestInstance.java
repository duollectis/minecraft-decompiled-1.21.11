package net.minecraft.test;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TestBlock;
import net.minecraft.block.entity.TestBlockEntity;
import net.minecraft.block.enums.TestBlockMode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Реализация тестового инстанса, управляемая блоками {@code TEST_BLOCK} в структуре.
 * <p>
 * Логика теста определяется расстановкой блоков с режимами {@link TestBlockMode}:
 * {@code START} — запускает тест, {@code ACCEPT} — фиксирует успех, {@code FAIL} — фиксирует провал.
 */
public class BlockBasedTestInstance extends TestInstance {

	public static final MapCodec<BlockBasedTestInstance> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(TestData.CODEC.forGetter(TestInstance::getData))
					.apply(instance, BlockBasedTestInstance::new)
	);

	public BlockBasedTestInstance(TestData<RegistryEntry<TestEnvironmentDefinition>> testData) {
		super(testData);
	}

	/**
	 * Запускает тест: активирует стартовый блок и на каждом тике проверяет
	 * состояние блоков ACCEPT/FAIL.
	 */
	@Override
	public void start(TestContext context) {
		BlockPos startPos = findStartBlockPos(context);
		TestBlockEntity startBlock = context.getBlockEntity(startPos, TestBlockEntity.class);
		startBlock.trigger();

		context.forEachRemainingTick(() -> {
			List<BlockPos> acceptBlocks = findTestBlocks(context, TestBlockMode.ACCEPT);
			if (acceptBlocks.isEmpty()) {
				context.throwGameTestException(Text.translatable(
						"test_block.error.missing",
						TestBlockMode.ACCEPT.getName()
				));
			}

			boolean anyAccepted = acceptBlocks.stream()
					.map(pos -> context.getBlockEntity(pos, TestBlockEntity.class))
					.anyMatch(TestBlockEntity::hasTriggered);

			if (anyAccepted) {
				context.complete();
			} else {
				handleTrigger(
						context,
						TestBlockMode.FAIL,
						entity -> context.throwGameTestException(Text.literal(entity.getMessage()))
				);
				handleTrigger(context, TestBlockMode.LOG, TestBlockEntity::trigger);
			}
		});
	}

	@Override
	public MapCodec<BlockBasedTestInstance> getCodec() {
		return CODEC;
	}

	@Override
	protected MutableText getTypeDescription() {
		return Text.translatable("test_instance.type.block_based");
	}

	private void handleTrigger(TestContext context, TestBlockMode mode, Consumer<TestBlockEntity> callback) {
		for (BlockPos pos : findTestBlocks(context, mode)) {
			TestBlockEntity entity = context.getBlockEntity(pos, TestBlockEntity.class);
			if (entity.hasTriggered()) {
				callback.accept(entity);
				entity.reset();
			}
		}
	}

	private BlockPos findStartBlockPos(TestContext context) {
		List<BlockPos> startBlocks = findTestBlocks(context, TestBlockMode.START);
		if (startBlocks.isEmpty()) {
			context.throwGameTestException(Text.translatable(
					"test_block.error.missing",
					TestBlockMode.START.getName()
			));
		}

		if (startBlocks.size() != 1) {
			context.throwGameTestException(Text.translatable(
					"test_block.error.too_many",
					TestBlockMode.START.getName()
			));
		}

		return startBlocks.getFirst();
	}

	private List<BlockPos> findTestBlocks(TestContext context, TestBlockMode mode) {
		List<BlockPos> result = new ArrayList<>();
		context.forEachRelativePos(pos -> {
			BlockState state = context.getBlockState(pos);
			if (state.isOf(Blocks.TEST_BLOCK) && state.get(TestBlock.MODE) == mode) {
				result.add(pos.toImmutable());
			}
		});
		return result;
	}
}
