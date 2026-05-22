package net.minecraft.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.JigsawStructure;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Структурный фрагмент, порождённый jigsaw-генератором на основе {@link StructurePoolElement}.
 * Хранит элемент пула, позицию, поворот, список стыков {@link JigsawJunction}
 * и настройки жидкостей, необходимые для корректного размещения и расчёта плотности шума.
 */
public class PoolStructurePiece extends StructurePiece {

	protected final StructurePoolElement poolElement;
	protected BlockPos pos;
	private final int groundLevelDelta;
	protected final BlockRotation rotation;
	private final List<JigsawJunction> junctions = Lists.newArrayList();
	private final StructureTemplateManager structureTemplateManager;
	private final StructureLiquidSettings liquidSettings;

	public PoolStructurePiece(
		StructureTemplateManager structureTemplateManager,
		StructurePoolElement poolElement,
		BlockPos pos,
		int groundLevelDelta,
		BlockRotation rotation,
		BlockBox boundingBox,
		StructureLiquidSettings liquidSettings
	) {
		super(StructurePieceType.JIGSAW, 0, boundingBox);
		this.structureTemplateManager = structureTemplateManager;
		this.poolElement = poolElement;
		this.pos = pos;
		this.groundLevelDelta = groundLevelDelta;
		this.rotation = rotation;
		this.liquidSettings = liquidSettings;
	}

	public PoolStructurePiece(StructureContext context, NbtCompound nbt) {
		super(StructurePieceType.JIGSAW, nbt);
		structureTemplateManager = context.structureTemplateManager();
		pos = new BlockPos(nbt.getInt("PosX", 0), nbt.getInt("PosY", 0), nbt.getInt("PosZ", 0));
		groundLevelDelta = nbt.getInt("ground_level_delta", 0);

		DynamicOps<NbtElement> dynamicOps = context.registryManager().getOps(NbtOps.INSTANCE);
		poolElement = nbt.<StructurePoolElement>get("pool_element", StructurePoolElement.CODEC, dynamicOps)
			.orElseThrow(() -> new IllegalStateException("Invalid pool element found"));
		rotation = nbt.<BlockRotation>get("rotation", BlockRotation.ENUM_NAME_CODEC).orElseThrow();
		boundingBox = poolElement.getBoundingBox(structureTemplateManager, pos, rotation);

		NbtList junctionList = nbt.getListOrEmpty("junctions");
		junctions.clear();
		junctionList.forEach(tag -> junctions.add(JigsawJunction.deserialize(new Dynamic<>(dynamicOps, tag))));

		liquidSettings = nbt.<StructureLiquidSettings>get("liquid_settings", StructureLiquidSettings.codec)
			.orElse(JigsawStructure.DEFAULT_LIQUID_SETTINGS);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putInt("PosX", pos.getX());
		nbt.putInt("PosY", pos.getY());
		nbt.putInt("PosZ", pos.getZ());
		nbt.putInt("ground_level_delta", groundLevelDelta);

		DynamicOps<NbtElement> dynamicOps = context.registryManager().getOps(NbtOps.INSTANCE);
		nbt.put("pool_element", StructurePoolElement.CODEC, dynamicOps, poolElement);
		nbt.put("rotation", BlockRotation.ENUM_NAME_CODEC, rotation);

		NbtList junctionList = new NbtList();
		for (JigsawJunction junction : junctions) {
			junctionList.add((NbtElement) junction.serialize(dynamicOps).getValue());
		}

		nbt.put("junctions", junctionList);

		if (liquidSettings != JigsawStructure.DEFAULT_LIQUID_SETTINGS) {
			nbt.put("liquid_settings", StructureLiquidSettings.codec, dynamicOps, liquidSettings);
		}
	}

	@Override
	public void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	) {
		generate(world, structureAccessor, chunkGenerator, random, chunkBox, pivot, false);
	}

	/**
	 * Размещает элемент пула в мире с возможностью сохранения jigsaw-блоков.
	 *
	 * @param keepJigsaws если {@code true}, jigsaw-блоки не заменяются финальными состояниями
	 */
	public void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox boundingBox,
		BlockPos pivot,
		boolean keepJigsaws
	) {
		poolElement.generate(
			structureTemplateManager,
			world,
			structureAccessor,
			chunkGenerator,
			pos,
			pivot,
			rotation,
			boundingBox,
			random,
			liquidSettings,
			keepJigsaws
		);
	}

	@Override
	public void translate(int x, int y, int z) {
		super.translate(x, y, z);
		pos = pos.add(x, y, z);
	}

	@Override
	public BlockRotation getRotation() {
		return rotation;
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"<%s | %s | %s | %s>",
			getClass().getSimpleName(),
			pos,
			rotation,
			poolElement
		);
	}

	public StructurePoolElement getPoolElement() {
		return poolElement;
	}

	public BlockPos getPos() {
		return pos;
	}

	public int getGroundLevelDelta() {
		return groundLevelDelta;
	}

	public void addJunction(JigsawJunction junction) {
		junctions.add(junction);
	}

	public List<JigsawJunction> getJunctions() {
		return Collections.unmodifiableList(junctions);
	}
}
