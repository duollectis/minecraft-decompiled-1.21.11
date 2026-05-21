package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
/**
 * {@code DiffuseLighting}.
 */
public class DiffuseLighting implements AutoCloseable {

	private static final Vector3f DEFAULT_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
	private static final Vector3f DEFAULT_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
	private static final Vector3f DARKENED_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
	private static final Vector3f DARKENED_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();
	private static final Vector3f INVENTORY_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, -1.0F, 1.0F).normalize();
	private static final Vector3f INVENTORY_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();
	public static final int UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
	private final GpuBuffer buffer;
	private final long roundedUboSize;

	public DiffuseLighting() {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.roundedUboSize = MathHelper.roundUpToMultiple(UBO_SIZE, gpuDevice.getUniformOffsetAlignment());
		this.buffer =
				gpuDevice.createBuffer(
						() -> "Lighting UBO",
						136,
						this.roundedUboSize * DiffuseLighting.Type.values().length
				);
		Matrix4f matrix4f = new Matrix4f().rotationY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
		this.updateBuffer(
				DiffuseLighting.Type.ITEMS_FLAT,
				matrix4f.transformDirection(DEFAULT_DIFFUSION_LIGHT_0, new Vector3f()),
				matrix4f.transformDirection(DEFAULT_DIFFUSION_LIGHT_1, new Vector3f())
		);
		Matrix4f matrix4f2 = new Matrix4f()
				.scaling(1.0F, -1.0F, 1.0F)
				.rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
				.rotateYXZ((float) (-Math.PI / 8), (float) (Math.PI * 3.0 / 4.0), 0.0F);
		this.updateBuffer(
				DiffuseLighting.Type.ITEMS_3D,
				matrix4f2.transformDirection(DEFAULT_DIFFUSION_LIGHT_0, new Vector3f()),
				matrix4f2.transformDirection(DEFAULT_DIFFUSION_LIGHT_1, new Vector3f())
		);
		this.updateBuffer(DiffuseLighting.Type.ENTITY_IN_UI, INVENTORY_DIFFUSION_LIGHT_0, INVENTORY_DIFFUSION_LIGHT_1);
		Matrix4f matrix4f3 = new Matrix4f();
		this.updateBuffer(
				DiffuseLighting.Type.PLAYER_SKIN,
				matrix4f3.transformDirection(INVENTORY_DIFFUSION_LIGHT_0, new Vector3f()),
				matrix4f3.transformDirection(INVENTORY_DIFFUSION_LIGHT_1, new Vector3f())
		);
	}

	/**
	 * Обновляет level buffer.
	 *
	 * @param cardinalLightType cardinal light type
	 */
	public void updateLevelBuffer(DimensionType.CardinalLightType cardinalLightType) {
		switch (cardinalLightType) {
			case DEFAULT:
				this.updateBuffer(DiffuseLighting.Type.LEVEL, DEFAULT_DIFFUSION_LIGHT_0, DEFAULT_DIFFUSION_LIGHT_1);
				break;
			case NETHER:
				this.updateBuffer(DiffuseLighting.Type.LEVEL, DARKENED_DIFFUSION_LIGHT_0, DARKENED_DIFFUSION_LIGHT_1);
		}
	}

	private void updateBuffer(DiffuseLighting.Type type, Vector3f light0Diffusion, Vector3f light1Diffusion) {
		MemoryStack memoryStack = MemoryStack.stackPush();

		try {
			ByteBuffer
					byteBuffer =
					Std140Builder
							.onStack(memoryStack, UBO_SIZE)
							.putVec3(light0Diffusion)
							.putVec3(light1Diffusion)
							.get();
			RenderSystem.getDevice()
			            .createCommandEncoder()
			            .writeToBuffer(
					            this.buffer.slice(type.ordinal() * this.roundedUboSize, this.roundedUboSize),
					            byteBuffer
			            );
		}
		catch (Throwable var8) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				}
				catch (Throwable var7) {
					var8.addSuppressed(var7);
				}
			}

			throw var8;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}
	}

	public void setShaderLights(DiffuseLighting.Type type) {
		RenderSystem.setShaderLights(this.buffer.slice(type.ordinal() * this.roundedUboSize, UBO_SIZE));
	}

	@Override
	public void close() {
		this.buffer.close();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Type}.
	 */
	public static enum Type {
		LEVEL,
		ITEMS_FLAT,
		ITEMS_3D,
		ENTITY_IN_UI,
		PLAYER_SKIN;
	}
}
