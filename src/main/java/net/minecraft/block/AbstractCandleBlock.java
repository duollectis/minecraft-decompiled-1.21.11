package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

public abstract class AbstractCandleBlock extends Block {

	public static final int LUMINANCE_PER_CANDLE = 3;
	public static final BooleanProperty LIT = Properties.LIT;

	/** Порог вероятности появления дыма при случайном тике отображения. */
	private static final float SMOKE_CHANCE = 0.3F;
	/** Порог вероятности воспроизведения звука горящей свечи. */
	private static final float SOUND_CHANCE = 0.17F;
	/** Флаг обновления блока: уведомить соседей + отправить клиентам. */
	private static final int BLOCK_UPDATE_FLAGS = 11;

	protected AbstractCandleBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected abstract MapCodec<? extends AbstractCandleBlock> getCodec();

	/**
	 * Возвращает набор смещений (относительно позиции блока), в которых
	 * должны появляться частицы пламени и дыма. Количество смещений
	 * соответствует числу свечей в блоке (от 1 до 4).
	 */
	protected abstract Iterable<Vec3d> getParticleOffsets(BlockState state);

	/**
	 * Проверяет, является ли данное состояние блока горящей свечой.
	 * Учитывает оба варианта размещения: отдельная свеча и свеча на торте,
	 * поскольку оба тега ({@code CANDLES} и {@code CANDLE_CAKES}) обрабатываются единообразно.
	 */
	public static boolean isLitCandle(BlockState state) {
		return state.contains(LIT)
			&& (state.isIn(BlockTags.CANDLES) || state.isIn(BlockTags.CANDLE_CAKES))
			&& state.get(LIT);
	}

	/**
	 * Поджигает незажжённую свечу при попадании горящего снаряда.
	 * Срабатывает только на сервере, чтобы избежать дублирования эффекта.
	 */
	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		if (world.isClient() || !projectile.isOnFire() || state.get(LIT)) {
			return;
		}

		setLit(world, state, hit.getBlockPos(), true);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (!state.get(LIT)) {
			return;
		}

		getParticleOffsets(state)
			.forEach(offset -> spawnCandleParticles(
				world,
				offset.add(pos.getX(), pos.getY(), pos.getZ()),
				random
			));
	}

	/**
	 * Гасит свечу: снимает флаг {@link #LIT}, испускает частицы дыма
	 * и звук тушения, а также генерирует игровое событие {@link GameEvent#BLOCK_CHANGE}.
	 */
	public static void extinguish(@Nullable PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos) {
		setLit(world, state, pos, false);

		if (state.getBlock() instanceof AbstractCandleBlock candleBlock) {
			candleBlock.getParticleOffsets(state)
				.forEach(offset -> world.addParticleClient(
					ParticleTypes.SMOKE,
					pos.getX() + offset.getX(),
					pos.getY() + offset.getY(),
					pos.getZ() + offset.getZ(),
					0.0,
					0.1F,
					0.0
				));
		}

		world.playSound(null, pos, SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/**
	 * Гасит свечу при взрыве, если взрыв способен активировать блоки
	 * (например, TNT, но не взрыв криппера с {@code canTriggerBlocks = false}).
	 * После тушения делегирует стандартную логику дропа родительскому классу.
	 */
	@Override
	protected void onExploded(
		BlockState state,
		ServerWorld world,
		BlockPos pos,
		Explosion explosion,
		BiConsumer<ItemStack, BlockPos> stackMerger
	) {
		if (explosion.canTriggerBlocks() && state.get(LIT)) {
			extinguish(null, state, world, pos);
		}

		super.onExploded(state, world, pos, explosion, stackMerger);
	}

	private static void setLit(WorldAccess world, BlockState state, BlockPos pos, boolean lit) {
		world.setBlockState(pos, state.with(LIT, lit), BLOCK_UPDATE_FLAGS);
	}

	private static void spawnCandleParticles(World world, Vec3d position, Random random) {
		float chance = random.nextFloat();

		if (chance < SMOKE_CHANCE) {
			world.addParticleClient(ParticleTypes.SMOKE, position.x, position.y, position.z, 0.0, 0.0, 0.0);

			if (chance < SOUND_CHANCE) {
				world.playSoundClient(
					position.x + 0.5,
					position.y + 0.5,
					position.z + 0.5,
					SoundEvents.BLOCK_CANDLE_AMBIENT,
					SoundCategory.BLOCKS,
					1.0F + random.nextFloat(),
					random.nextFloat() * 0.7F + 0.3F,
					false
				);
			}
		}

		world.addParticleClient(ParticleTypes.SMALL_FLAME, position.x, position.y, position.z, 0.0, 0.0, 0.0);
	}
}
