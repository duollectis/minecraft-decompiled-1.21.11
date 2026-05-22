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

/**
 * Управляет диффузным освещением для различных контекстов рендеринга (мир, инвентарь, UI).
 * Хранит GPU-буфер с направлениями двух источников диффузного света для каждого типа освещения.
 * Буфер обновляется при смене измерения (DEFAULT/NETHER) и инициализируется при создании
 * для всех статических контекстов (предметы, сущности в UI, скин игрока).
 */
@Environment(EnvType.CLIENT)
public class DiffuseLighting implements AutoCloseable {

	private static final Vector3f DEFAULT_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
	private static final Vector3f DEFAULT_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
	private static final Vector3f DARKENED_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
	private static final Vector3f DARKENED_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();
	private static final Vector3f INVENTORY_DIFFUSION_LIGHT_0 = new Vector3f(0.2F, -1.0F, 1.0F).normalize();
	private static final Vector3f INVENTORY_DIFFUSION_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();
	/** Флаги создания GPU-буфера: MAP_WRITE | COPY_DST (0x80 | 0x08 = 136). */
	private static final int GPU_BUFFER_FLAGS = 136;

	public static final int UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();

	private final GpuBuffer buffer;
	private final long roundedUboSize;

	public DiffuseLighting() {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		roundedUboSize = MathHelper.roundUpToMultiple(UBO_SIZE, gpuDevice.getUniformOffsetAlignment());
		buffer = gpuDevice.createBuffer(
				() -> "Lighting UBO",
				GPU_BUFFER_FLAGS,
				roundedUboSize * DiffuseLighting.Type.values().length
		);

		Matrix4f flatItemTransform = new Matrix4f()
				.rotationY((float) (-Math.PI / 8))
				.rotateX((float) (Math.PI * 3.0 / 4.0));
		updateBuffer(
				DiffuseLighting.Type.ITEMS_FLAT,
				flatItemTransform.transformDirection(DEFAULT_DIFFUSION_LIGHT_0, new Vector3f()),
				flatItemTransform.transformDirection(DEFAULT_DIFFUSION_LIGHT_1, new Vector3f())
		);

		Matrix4f item3dTransform = new Matrix4f()
				.scaling(1.0F, -1.0F, 1.0F)
				.rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
				.rotateYXZ((float) (-Math.PI / 8), (float) (Math.PI * 3.0 / 4.0), 0.0F);
		updateBuffer(
				DiffuseLighting.Type.ITEMS_3D,
				item3dTransform.transformDirection(DEFAULT_DIFFUSION_LIGHT_0, new Vector3f()),
				item3dTransform.transformDirection(DEFAULT_DIFFUSION_LIGHT_1, new Vector3f())
		);

		updateBuffer(DiffuseLighting.Type.ENTITY_IN_UI, INVENTORY_DIFFUSION_LIGHT_0, INVENTORY_DIFFUSION_LIGHT_1);

		Matrix4f skinTransform = new Matrix4f();
		updateBuffer(
				DiffuseLighting.Type.PLAYER_SKIN,
				skinTransform.transformDirection(INVENTORY_DIFFUSION_LIGHT_0, new Vector3f()),
				skinTransform.transformDirection(INVENTORY_DIFFUSION_LIGHT_1, new Vector3f())
		);
	}

	/**
	 * Обновляет секцию буфера для типа {@link Type#LEVEL} в зависимости от типа измерения.
	 * Нижний мир использует затемнённое освещение с инвертированным вторым источником.
	 *
	 * @param cardinalLightType тип кардинального освещения измерения
	 */
	public void updateLevelBuffer(DimensionType.CardinalLightType cardinalLightType) {
		switch (cardinalLightType) {
			case DEFAULT:
				updateBuffer(DiffuseLighting.Type.LEVEL, DEFAULT_DIFFUSION_LIGHT_0, DEFAULT_DIFFUSION_LIGHT_1);
				break;
			case NETHER:
				updateBuffer(DiffuseLighting.Type.LEVEL, DARKENED_DIFFUSION_LIGHT_0, DARKENED_DIFFUSION_LIGHT_1);
				break;
		}
	}

	private void updateBuffer(DiffuseLighting.Type type, Vector3f light0, Vector3f light1) {
		MemoryStack memoryStack = MemoryStack.stackPush();

		try {
			ByteBuffer uboData = Std140Builder.onStack(memoryStack, UBO_SIZE)
					.putVec3(light0)
					.putVec3(light1)
					.get();
			RenderSystem.getDevice()
					.createCommandEncoder()
					.writeToBuffer(
							buffer.slice(type.ordinal() * roundedUboSize, roundedUboSize),
							uboData
					);
		}
		catch (Throwable throwable) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				}
				catch (Throwable suppressed) {
					throwable.addSuppressed(suppressed);
				}
			}

			throw throwable;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}
	}

	public void setShaderLights(DiffuseLighting.Type type) {
		RenderSystem.setShaderLights(buffer.slice(type.ordinal() * roundedUboSize, UBO_SIZE));
	}

	@Override
	public void close() {
		buffer.close();
	}

	/** Перечисление контекстов диффузного освещения, каждый хранится в отдельной секции UBO. */
	@Environment(EnvType.CLIENT)
	public enum Type {
		LEVEL,
		ITEMS_FLAT,
		ITEMS_3D,
		ENTITY_IN_UI,
		PLAYER_SKIN
	}
}
