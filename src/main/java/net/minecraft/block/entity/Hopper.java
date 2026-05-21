package net.minecraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.Box;

/**
 * {@code Hopper}.
 */
public interface Hopper extends Inventory {

	Box INPUT_AREA_SHAPE = Block.createColumnShape(16.0, 11.0, 32.0).getBoundingBoxes().get(0);

	default Box getInputAreaShape() {
		return INPUT_AREA_SHAPE;
	}

	double getHopperX();

	double getHopperY();

	double getHopperZ();

	boolean canBlockFromAbove();
}
