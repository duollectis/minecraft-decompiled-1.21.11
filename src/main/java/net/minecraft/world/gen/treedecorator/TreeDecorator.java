package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;

import java.util.Comparator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Абстрактный декоратор дерева. Применяется после генерации ствола и листьев,
 * добавляя дополнительные блоки (лианы, ульи, какао, мох и т.д.).
 */
public abstract class TreeDecorator {

	public static final Codec<TreeDecorator> TYPE_CODEC = Registries.TREE_DECORATOR_TYPE
			.getCodec()
			.dispatch(TreeDecorator::getType, TreeDecoratorType::getCodec);

	protected abstract TreeDecoratorType<?> getType();

	public abstract void generate(TreeDecorator.Generator generator);

	/**
	 * Контекст генерации декоратора дерева. Предоставляет доступ к позициям брёвен,
	 * листьев и корней, а также методы для замены блоков в мире.
	 */
	public static final class Generator {

		private final TestableWorld world;
		private final BiConsumer<BlockPos, BlockState> replacer;
		private final Random random;
		private final ObjectArrayList<BlockPos> logPositions;
		private final ObjectArrayList<BlockPos> leavesPositions;
		private final ObjectArrayList<BlockPos> rootPositions;

		public Generator(
				TestableWorld world,
				BiConsumer<BlockPos, BlockState> replacer,
				Random random,
				Set<BlockPos> logPositions,
				Set<BlockPos> leavesPositions,
				Set<BlockPos> rootPositions
		) {
			this.world = world;
			this.replacer = replacer;
			this.random = random;
			this.rootPositions = new ObjectArrayList(rootPositions);
			this.logPositions = new ObjectArrayList(logPositions);
			this.leavesPositions = new ObjectArrayList(leavesPositions);
			this.logPositions.sort(Comparator.comparingInt(Vec3i::getY));
			this.leavesPositions.sort(Comparator.comparingInt(Vec3i::getY));
			this.rootPositions.sort(Comparator.comparingInt(Vec3i::getY));
		}

		public void replaceWithVine(BlockPos pos, BooleanProperty faceProperty) {
			replace(pos, Blocks.VINE.getDefaultState().with(faceProperty, true));
		}

		public void replace(BlockPos pos, BlockState state) {
			replacer.accept(pos, state);
		}

		public boolean isAir(BlockPos pos) {
			return world.testBlockState(pos, AbstractBlock.AbstractBlockState::isAir);
		}

		public boolean matches(BlockPos pos, Predicate<BlockState> statePredicate) {
			return world.testBlockState(pos, statePredicate);
		}

		public TestableWorld getWorld() {
			return world;
		}

		public Random getRandom() {
			return random;
		}

		public ObjectArrayList<BlockPos> getLogPositions() {
			return logPositions;
		}

		public ObjectArrayList<BlockPos> getLeavesPositions() {
			return leavesPositions;
		}

		public ObjectArrayList<BlockPos> getRootPositions() {
			return rootPositions;
		}
	}
}
