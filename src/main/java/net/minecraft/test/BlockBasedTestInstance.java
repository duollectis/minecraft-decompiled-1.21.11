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
 * {@code BlockBasedTestInstance}.
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

	@Override
	public void start(TestContext context) {
		BlockPos blockPos = this.findStartBlockPos(context);
		TestBlockEntity testBlockEntity = context.getBlockEntity(blockPos, TestBlockEntity.class);
		testBlockEntity.trigger();
		context.forEachRemainingTick(() -> {
			List<BlockPos> list = this.findTestBlocks(context, TestBlockMode.ACCEPT);
			if (list.isEmpty()) {
				context.throwGameTestException(Text.translatable(
						"test_block.error.missing",
						TestBlockMode.ACCEPT.getName()
				));
			}

			boolean
					bl =
					list
							.stream()
							.map(pos -> context.getBlockEntity(pos, TestBlockEntity.class))
							.anyMatch(TestBlockEntity::hasTriggered);
			if (bl) {
				context.complete();
			}
			else {
				this.handleTrigger(
						context,
						TestBlockMode.FAIL,
						testBlockEntityx -> context.throwGameTestException(Text.literal(testBlockEntityx.getMessage()))
				);
				this.handleTrigger(context, TestBlockMode.LOG, TestBlockEntity::trigger);
			}
		});
	}

	private void handleTrigger(TestContext context, TestBlockMode mode, Consumer<TestBlockEntity> callback) {
		for (BlockPos blockPos : this.findTestBlocks(context, mode)) {
			TestBlockEntity testBlockEntity = context.getBlockEntity(blockPos, TestBlockEntity.class);
			if (testBlockEntity.hasTriggered()) {
				callback.accept(testBlockEntity);
				testBlockEntity.reset();
			}
		}
	}

	private BlockPos findStartBlockPos(TestContext context) {
		List<BlockPos> list = this.findTestBlocks(context, TestBlockMode.START);
		if (list.isEmpty()) {
			context.throwGameTestException(Text.translatable(
					"test_block.error.missing",
					TestBlockMode.START.getName()
			));
		}

		if (list.size() != 1) {
			context.throwGameTestException(Text.translatable(
					"test_block.error.too_many",
					TestBlockMode.START.getName()
			));
		}

		return list.getFirst();
	}

	private List<BlockPos> findTestBlocks(TestContext context, TestBlockMode mode) {
		List<BlockPos> list = new ArrayList<>();
		context.forEachRelativePos(pos -> {
			BlockState blockState = context.getBlockState(pos);
			if (blockState.isOf(Blocks.TEST_BLOCK) && blockState.get(TestBlock.MODE) == mode) {
				list.add(pos.toImmutable());
			}
		});
		return list;
	}

	@Override
	public MapCodec<BlockBasedTestInstance> getCodec() {
		return CODEC;
	}

	@Override
	protected MutableText getTypeDescription() {
		return Text.translatable("test_instance.type.block_based");
	}
}
