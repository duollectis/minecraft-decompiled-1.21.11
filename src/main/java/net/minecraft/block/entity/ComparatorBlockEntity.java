package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

/**
 * Блок-сущность компаратора. Хранит последнее вычисленное выходное значение сигнала.
 */
public class ComparatorBlockEntity extends BlockEntity {

	private int outputSignal;

	public ComparatorBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.COMPARATOR, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putInt("OutputSignal", outputSignal);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		outputSignal = view.getInt("OutputSignal", 0);
	}

	public int getOutputSignal() {
		return outputSignal;
	}

	public void setOutputSignal(int outputSignal) {
		this.outputSignal = outputSignal;
	}
}
