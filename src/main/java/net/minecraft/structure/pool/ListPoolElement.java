package net.minecraft.structure.pool;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Составной элемент пула, объединяющий несколько {@link StructurePoolElement} в один.
 * При генерации все дочерние элементы размещаются последовательно в одной позиции.
 * Ограничивающий прямоугольник вычисляется как объединение прямоугольников всех дочерних элементов.
 * Jigsaw-блоки берутся только от первого элемента списка.
 */
public class ListPoolElement extends StructurePoolElement {

	public static final MapCodec<ListPoolElement> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter(pool -> pool.elements),
				projectionGetter()
			)
			.apply(instance, ListPoolElement::new)
	);

	private final List<StructurePoolElement> elements;

	public ListPoolElement(List<StructurePoolElement> elements, StructurePool.Projection projection) {
		super(projection);
		if (elements.isEmpty()) {
			throw new IllegalArgumentException("Elements are empty");
		}

		this.elements = elements;
		setAllElementsProjection(projection);
	}

	@Override
	public Vec3i getStart(StructureTemplateManager structureTemplateManager, BlockRotation rotation) {
		int maxX = 0;
		int maxY = 0;
		int maxZ = 0;

		for (StructurePoolElement element : elements) {
			Vec3i size = element.getStart(structureTemplateManager, rotation);
			maxX = Math.max(maxX, size.getX());
			maxY = Math.max(maxY, size.getY());
			maxZ = Math.max(maxZ, size.getZ());
		}

		return new Vec3i(maxX, maxY, maxZ);
	}

	@Override
	public List<StructureTemplate.JigsawBlockInfo> getStructureBlockInfos(
		StructureTemplateManager structureTemplateManager,
		BlockPos pos,
		BlockRotation rotation,
		Random random
	) {
		return elements.get(0).getStructureBlockInfos(structureTemplateManager, pos, rotation, random);
	}

	@Override
	public BlockBox getBoundingBox(
		StructureTemplateManager structureTemplateManager,
		BlockPos pos,
		BlockRotation rotation
	) {
		return BlockBox.encompass(
			elements.stream()
				.filter(element -> element != EmptyPoolElement.INSTANCE)
				.map(element -> element.getBoundingBox(structureTemplateManager, pos, rotation))
				::iterator
		).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox for ListPoolElement"));
	}

	@Override
	public boolean generate(
		StructureTemplateManager structureTemplateManager,
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		BlockPos pos,
		BlockPos pivot,
		BlockRotation rotation,
		BlockBox box,
		Random random,
		StructureLiquidSettings liquidSettings,
		boolean keepJigsaws
	) {
		for (StructurePoolElement element : elements) {
			if (!element.generate(
				structureTemplateManager,
				world,
				structureAccessor,
				chunkGenerator,
				pos,
				pivot,
				rotation,
				box,
				random,
				liquidSettings,
				keepJigsaws
			)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public StructurePoolElementType<?> getType() {
		return StructurePoolElementType.LIST_POOL_ELEMENT;
	}

	@Override
	public StructurePoolElement setProjection(StructurePool.Projection projection) {
		super.setProjection(projection);
		setAllElementsProjection(projection);
		return this;
	}

	@Override
	public String toString() {
		return "List[" + elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
	}

	private void setAllElementsProjection(StructurePool.Projection projection) {
		elements.forEach(element -> element.setProjection(projection));
	}

	@VisibleForTesting
	public List<StructurePoolElement> getElements() {
		return elements;
	}
}
