package net.minecraft.structure.pool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.GravityStructureProcessor;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.function.Function;

/**
 * Пул структурных элементов для jigsaw-генерации.
 * Хранит взвешенный список {@link StructurePoolElement} и ссылку на резервный пул {@code fallback},
 * используемый когда ни один элемент текущего пула не подходит для размещения.
 */
public class StructurePool {

	private static final int UNSET_HIGHEST_Y = Integer.MIN_VALUE;
	private static final MutableObject<Codec<RegistryEntry<StructurePool>>> FALLBACK_CODEC_REF = new MutableObject<>();

	public static final Codec<StructurePool> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.lazyInitialized(FALLBACK_CODEC_REF).fieldOf("fallback").forGetter(StructurePool::getFallback),
			Codec.mapPair(
				StructurePoolElement.CODEC.fieldOf("element"),
				Codec.intRange(1, 150).fieldOf("weight")
			).codec().listOf().fieldOf("elements").forGetter(pool -> pool.elementWeights)
		).apply(instance, StructurePool::new)
	);

	public static final Codec<RegistryEntry<StructurePool>> REGISTRY_CODEC = Util.make(
		RegistryElementCodec.of(RegistryKeys.TEMPLATE_POOL, CODEC),
		FALLBACK_CODEC_REF::setValue
	);

	private final List<Pair<StructurePoolElement, Integer>> elementWeights;
	private final ObjectArrayList<StructurePoolElement> elements;
	private final RegistryEntry<StructurePool> fallback;
	private int highestY = UNSET_HIGHEST_Y;

	/**
	 * Создаёт пул из взвешенного списка элементов.
	 * Каждый элемент добавляется в плоский список {@code elements} столько раз, каков его вес,
	 * что позволяет выбирать случайный элемент через {@code random.nextInt(size)}.
	 */
	public StructurePool(RegistryEntry<StructurePool> fallback, List<Pair<StructurePoolElement, Integer>> elementWeights) {
		this.elementWeights = elementWeights;
		elements = new ObjectArrayList<>();
		this.fallback = fallback;

		for (Pair<StructurePoolElement, Integer> pair : elementWeights) {
			StructurePoolElement element = pair.getFirst();
			int weight = pair.getSecond();
			for (int i = 0; i < weight; i++) {
				elements.add(element);
			}
		}
	}

	/**
	 * Создаёт пул из фабричных функций элементов с заданной проекцией.
	 * Используется при программном создании пулов в датагенераторах.
	 */
	public StructurePool(
		RegistryEntry<StructurePool> fallback,
		List<Pair<Function<StructurePool.Projection, ? extends StructurePoolElement>, Integer>> elementGetters,
		StructurePool.Projection projection
	) {
		elementWeights = Lists.newArrayList();
		elements = new ObjectArrayList<>();
		this.fallback = fallback;

		for (Pair<Function<StructurePool.Projection, ? extends StructurePoolElement>, Integer> pair : elementGetters) {
			StructurePoolElement element = pair.getFirst().apply(projection);
			int weight = pair.getSecond();
			elementWeights.add(Pair.of(element, weight));
			for (int i = 0; i < weight; i++) {
				elements.add(element);
			}
		}
	}

	/**
	 * Возвращает максимальную высоту (в блоках) среди всех элементов пула.
	 * Результат кэшируется после первого вычисления.
	 */
	public int getHighestY(StructureTemplateManager structureTemplateManager) {
		if (highestY == UNSET_HIGHEST_Y) {
			highestY = elements.stream()
				.filter(element -> element != EmptyPoolElement.INSTANCE)
				.mapToInt(element -> element
					.getBoundingBox(structureTemplateManager, BlockPos.ORIGIN, BlockRotation.NONE)
					.getBlockCountY()
				)
				.max()
				.orElse(0);
		}

		return highestY;
	}

	@VisibleForTesting
	public List<Pair<StructurePoolElement, Integer>> getElementWeights() {
		return elementWeights;
	}

	public RegistryEntry<StructurePool> getFallback() {
		return fallback;
	}

	public StructurePoolElement getRandomElement(Random random) {
		return elements.isEmpty()
			? EmptyPoolElement.INSTANCE
			: elements.get(random.nextInt(elements.size()));
	}

	public List<StructurePoolElement> getElementIndicesInRandomOrder(Random random) {
		return Util.copyShuffled(elements, random);
	}

	public int getElementCount() {
		return elements.size();
	}

	/**
	 * Режим проекции элемента пула на рельеф.
	 * {@code TERRAIN_MATCHING} применяет {@link GravityStructureProcessor} для подгонки под поверхность,
	 * {@code RIGID} размещает элемент без изменений по высоте.
	 */
	public enum Projection implements StringIdentifiable {
		TERRAIN_MATCHING(
			"terrain_matching",
			ImmutableList.of(new GravityStructureProcessor(Heightmap.Type.WORLD_SURFACE_WG, -1))
		),
		RIGID("rigid", ImmutableList.of());

		public static final StringIdentifiable.EnumCodec<StructurePool.Projection> CODEC =
			StringIdentifiable.createCodec(StructurePool.Projection::values);

		private final String id;
		private final ImmutableList<StructureProcessor> processors;

		Projection(String id, ImmutableList<StructureProcessor> processors) {
			this.id = id;
			this.processors = processors;
		}

		public String getId() {
			return id;
		}

		public static StructurePool.Projection getById(String id) {
			return CODEC.byId(id);
		}

		public ImmutableList<StructureProcessor> getProcessors() {
			return processors;
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
