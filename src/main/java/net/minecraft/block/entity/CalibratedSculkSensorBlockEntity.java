package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.CalibratedSculkSensorBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.Vibrations;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность откалиброванного датчика скалька. Расширяет обычный датчик,
 * добавляя фильтрацию вибраций по частоте, задаваемой входным сигналом редстоуна.
 */
public class CalibratedSculkSensorBlockEntity extends SculkSensorBlockEntity {

	private static final int CALIBRATED_RANGE = 16;

	public CalibratedSculkSensorBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BlockEntityType.CALIBRATED_SCULK_SENSOR, blockPos, blockState);
	}

	@Override
	public Vibrations.Callback createCallback() {
		return new CalibratedSculkSensorBlockEntity.Callback(getPos());
	}

	protected class Callback extends SculkSensorBlockEntity.VibrationCallback {

		public Callback(final BlockPos pos) {
			super(pos);
		}

		@Override
		public int getRange() {
			return CALIBRATED_RANGE;
		}

		/**
		 * Принимает вибрацию только если её частота совпадает с калибровочной частотой
		 * (входной сигнал редстоуна с противоположной стороны). Частота 0 означает «принимать всё».
		 */
		@Override
		public boolean accepts(
				ServerWorld world,
				BlockPos pos,
				RegistryEntry<GameEvent> event,
				GameEvent.@Nullable Emitter emitter
		) {
			int calibrationFrequency = getCalibrationFrequency(world, this.pos, getCachedState());
			if (calibrationFrequency != 0 && Vibrations.getFrequency(event) != calibrationFrequency) {
				return false;
			}

			return super.accepts(world, pos, event, emitter);
		}

		private int getCalibrationFrequency(World world, BlockPos pos, BlockState state) {
			Direction inputSide = state.get(CalibratedSculkSensorBlock.FACING).getOpposite();
			return world.getEmittedRedstonePower(pos.offset(inputSide), inputSide);
		}
	}
}
