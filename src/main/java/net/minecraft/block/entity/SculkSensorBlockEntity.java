package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.GameEventListener;
import org.jspecify.annotations.Nullable;

/**
 * Блок-сущность датчика скалька. Слушает вибрации в радиусе 8 блоков и активирует блок,
 * устанавливая частоту последней вибрации и силу сигнала компаратора.
 */
public class SculkSensorBlockEntity extends BlockEntity implements GameEventListener.Holder<Vibrations.VibrationListener>, Vibrations {

	private Vibrations.ListenerData listenerData;
	private final Vibrations.VibrationListener listener;
	private final Vibrations.Callback callback;
	private int lastVibrationFrequency = 0;

	protected SculkSensorBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
		callback = createCallback();
		listenerData = new Vibrations.ListenerData();
		listener = new Vibrations.VibrationListener(this);
	}

	public SculkSensorBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.SCULK_SENSOR, pos, state);
	}

	public Vibrations.Callback createCallback() {
		return new SculkSensorBlockEntity.VibrationCallback(getPos());
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		lastVibrationFrequency = view.getInt("last_vibration_frequency", 0);
		listenerData = view
			.<Vibrations.ListenerData>read("listener", Vibrations.ListenerData.CODEC)
			.orElseGet(Vibrations.ListenerData::new);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putInt("last_vibration_frequency", lastVibrationFrequency);
		view.put("listener", Vibrations.ListenerData.CODEC, listenerData);
	}

	@Override
	public Vibrations.ListenerData getVibrationListenerData() {
		return listenerData;
	}

	@Override
	public Vibrations.Callback getVibrationCallback() {
		return callback;
	}

	public int getLastVibrationFrequency() {
		return lastVibrationFrequency;
	}

	public void setLastVibrationFrequency(int lastVibrationFrequency) {
		this.lastVibrationFrequency = lastVibrationFrequency;
	}

	public Vibrations.VibrationListener getEventListener() {
		return listener;
	}

	/**
	 * Обратный вызов вибрации датчика скалька. Фильтрует события по частоте и состоянию блока,
	 * активируя датчик при получении допустимой вибрации.
	 */
	protected class VibrationCallback implements Vibrations.Callback {

		public static final int RANGE = 8;
		protected final BlockPos pos;
		private final PositionSource positionSource;

		public VibrationCallback(final BlockPos pos) {
			this.pos = pos;
			positionSource = new BlockPositionSource(pos);
		}

		@Override
		public int getRange() {
			return RANGE;
		}

		@Override
		public PositionSource getPositionSource() {
			return positionSource;
		}

		@Override
		public boolean triggersAvoidCriterion() {
			return true;
		}

		@Override
		public boolean accepts(
			ServerWorld world,
			BlockPos pos,
			RegistryEntry<GameEvent> event,
			GameEvent.@Nullable Emitter emitter
		) {
			if (pos.equals(this.pos)
				&& (event.matches(GameEvent.BLOCK_DESTROY) || event.matches(GameEvent.BLOCK_PLACE))
			) {
				return false;
			}

			return Vibrations.getFrequency(event) != 0
				&& SculkSensorBlock.isInactive(SculkSensorBlockEntity.this.getCachedState());
		}

		@Override
		public void accept(
			ServerWorld world,
			BlockPos pos,
			RegistryEntry<GameEvent> event,
			@Nullable Entity sourceEntity,
			@Nullable Entity entity,
			float distance
		) {
			BlockState sensorState = SculkSensorBlockEntity.this.getCachedState();

			if (!SculkSensorBlock.isInactive(sensorState)) {
				return;
			}

			int frequency = Vibrations.getFrequency(event);
			SculkSensorBlockEntity.this.setLastVibrationFrequency(frequency);

			int signalStrength = Vibrations.getSignalStrength(distance, getRange());

			if (sensorState.getBlock() instanceof SculkSensorBlock sculkSensorBlock) {
				sculkSensorBlock.setActive(sourceEntity, world, this.pos, sensorState, signalStrength, frequency);
			}
		}

		@Override
		public void onListen() {
			SculkSensorBlockEntity.this.markDirty();
		}

		@Override
		public boolean requiresTickingChunksAround() {
			return true;
		}
	}
}
