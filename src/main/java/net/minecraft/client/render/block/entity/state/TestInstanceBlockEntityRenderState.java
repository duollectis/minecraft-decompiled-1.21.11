package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.TestInstanceBlockEntity;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code TestInstanceBlockEntityRenderState}.
 */
public class TestInstanceBlockEntityRenderState extends BlockEntityRenderState {

	public BeaconBlockEntityRenderState beaconState;
	public StructureBlockBlockEntityRenderState structureState;
	public final List<TestInstanceBlockEntity.Error> errors = new ArrayList<>();
}
