package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.block.entity.state.BeaconBlockEntityRenderState;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.block.entity.state.StructureBlockBlockEntityRenderState;
import net.minecraft.client.render.block.entity.state.TestInstanceBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code TestInstanceBlockEntityRenderer}.
 */
public class TestInstanceBlockEntityRenderer implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceBlockEntityRenderState> {

	private static final float OUTLINE_OFFSET = 0.02F;
	private final BeaconBlockEntityRenderer<TestInstanceBlockEntity>
			beaconBlockEntityRenderer =
			new BeaconBlockEntityRenderer<>();
	private final StructureBlockBlockEntityRenderer<TestInstanceBlockEntity>
			structureBlockBlockEntityRenderer =
			new StructureBlockBlockEntityRenderer<>();

	/**
	 * Создаёт render state.
	 *
	 * @return TestInstanceBlockEntityRenderState — результат операции
	 */
	public TestInstanceBlockEntityRenderState createRenderState() {
		return new TestInstanceBlockEntityRenderState();
	}

	public void updateRenderState(
			TestInstanceBlockEntity testInstanceBlockEntity,
			TestInstanceBlockEntityRenderState testInstanceBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				testInstanceBlockEntity,
				testInstanceBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		testInstanceBlockEntityRenderState.beaconState = new BeaconBlockEntityRenderState();
		BlockEntityRenderState.updateBlockEntityRenderState(
				testInstanceBlockEntity,
				testInstanceBlockEntityRenderState.beaconState,
				crumblingOverlayCommand
		);
		BeaconBlockEntityRenderer.updateBeaconRenderState(
				testInstanceBlockEntity,
				testInstanceBlockEntityRenderState.beaconState,
				f,
				vec3d
		);
		testInstanceBlockEntityRenderState.structureState = new StructureBlockBlockEntityRenderState();
		BlockEntityRenderState.updateBlockEntityRenderState(
				testInstanceBlockEntity,
				testInstanceBlockEntityRenderState.structureState,
				crumblingOverlayCommand
		);
		StructureBlockBlockEntityRenderer.updateStructureBoxRenderState(
				testInstanceBlockEntity,
				testInstanceBlockEntityRenderState.structureState
		);
		testInstanceBlockEntityRenderState.errors.clear();

		for (TestInstanceBlockEntity.Error error : testInstanceBlockEntity.getErrors()) {
			testInstanceBlockEntityRenderState.errors.add(new TestInstanceBlockEntity.Error(error.pos(), error.text()));
		}
	}

	public void render(
			TestInstanceBlockEntityRenderState testInstanceBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		this.beaconBlockEntityRenderer.render(
				testInstanceBlockEntityRenderState.beaconState,
				matrixStack,
				orderedRenderCommandQueue,
				cameraRenderState
		);
		this.structureBlockBlockEntityRenderer
				.render(
						testInstanceBlockEntityRenderState.structureState,
						matrixStack,
						orderedRenderCommandQueue,
						cameraRenderState
				);

		for (TestInstanceBlockEntity.Error error : testInstanceBlockEntityRenderState.errors) {
			this.renderError(error);
		}
	}

	private void renderError(TestInstanceBlockEntity.Error error) {
		BlockPos blockPos = error.pos();
		GizmoDrawing.box(
				new Box(blockPos).expand(0.02F),
				DrawStyle.filled(ColorHelper.fromFloats(0.375F, 1.0F, 0.0F, 0.0F))
		);
		String string = error.text().getString();
		float f = 0.16F;
		GizmoDrawing
				.text(string, Vec3d.add(blockPos, 0.5, 1.2, 0.5), TextGizmo.Style.left().scaled(0.16F))
				.ignoreOcclusion();
	}

	@Override
	public boolean rendersOutsideBoundingBox() {
		return this.beaconBlockEntityRenderer.rendersOutsideBoundingBox()
				|| this.structureBlockBlockEntityRenderer.rendersOutsideBoundingBox();
	}

	@Override
	public int getRenderDistance() {
		return Math.max(
				this.beaconBlockEntityRenderer.getRenderDistance(),
				this.structureBlockBlockEntityRenderer.getRenderDistance()
		);
	}

	public boolean isInRenderDistance(TestInstanceBlockEntity testInstanceBlockEntity, Vec3d vec3d) {
		return this.beaconBlockEntityRenderer.isInRenderDistance(testInstanceBlockEntity, vec3d)
				|| this.structureBlockBlockEntityRenderer.isInRenderDistance(testInstanceBlockEntity, vec3d);
	}
}
