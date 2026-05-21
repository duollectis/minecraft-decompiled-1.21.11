package net.minecraft.client.render.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

/**
 * Коллектор ссылочных моделей. Разрешает иерархию родительских моделей,
 * обнаруживает циклические зависимости и собирает итоговую карту запечённых моделей.
 */
@Environment(EnvType.CLIENT)
public class ReferencedModelsCollector {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Object2ObjectMap<Identifier, ReferencedModelsCollector.Holder>
			modelCache =
			new Object2ObjectOpenHashMap<>();
	private final ReferencedModelsCollector.Holder missingModel;
	private final Object2ObjectFunction<Identifier, ReferencedModelsCollector.Holder> holderFactory;
	private final ResolvableModel.Resolver resolver;
	private final Queue<ReferencedModelsCollector.Holder> queue = new ArrayDeque<>();

	/**
	 * @param unbakedModels карта незапечённых моделей по идентификатору
	 * @param missingModel  модель-заглушка для отсутствующих ресурсов
	 */
	public ReferencedModelsCollector(Map<Identifier, UnbakedModel> unbakedModels, UnbakedModel missingModel) {
		this.missingModel = new ReferencedModelsCollector.Holder(MissingModel.ID, missingModel, true);
		this.modelCache.put(MissingModel.ID, this.missingModel);
		this.holderFactory = id -> {
			Identifier identifier = (Identifier) id;
			UnbakedModel unbakedModel = unbakedModels.get(identifier);

			if (unbakedModel == null) {
				LOGGER.warn("Missing block model: {}", identifier);
				return this.missingModel;
			}

			return this.schedule(identifier, unbakedModel);
		};
		this.resolver = this::resolve;
	}

	/**
	 * @return {@code true}, если модель является корневой (не имеет родителя)
	 */
	private static boolean isRootModel(UnbakedModel model) {
		return model.parent() == null;
	}

	/**
	 * Разрешает модель по идентификатору, используя кэш.
	 */
	private ReferencedModelsCollector.Holder resolve(Identifier id) {
		return (ReferencedModelsCollector.Holder) this.modelCache.computeIfAbsent(id, this.holderFactory);
	}

	/**
	 * Планирует разрешение родительской цепочки для некорневой модели.
	 */
	private ReferencedModelsCollector.Holder schedule(Identifier id, UnbakedModel model) {
		boolean bl = isRootModel(model);
		ReferencedModelsCollector.Holder newHolder = new ReferencedModelsCollector.Holder(id, model, bl);

		if (!bl) {
			this.queue.add(newHolder);
		}

		return newHolder;
	}

	/**
	 * Регистрирует модель для разрешения через резолвер.
	 *
	 * @param model модель для разрешения
	 */
	public void resolve(ResolvableModel model) {
		model.resolve(this.resolver);
	}

	/**
	 * Добавляет специальную корневую модель в кэш.
	 *
	 * @param id    идентификатор модели
	 * @param model незапечённая модель
	 */
	public void addSpecialModel(Identifier id, UnbakedModel model) {
		if (!isRootModel(model)) {
			LOGGER.warn("Trying to add non-root special model {}, ignoring", id);
			return;
		}

		ReferencedModelsCollector.Holder
				previous =
				(ReferencedModelsCollector.Holder) this.modelCache.put(id, this.schedule(id, model));

		if (previous != null) {
			LOGGER.warn("Duplicate special model {}", id);
		}
	}

	/**
	 * @return держатель модели-заглушки для отсутствующих ресурсов
	 */
	public BakedSimpleModel getMissingModel() {
		return this.missingModel;
	}

	/**
	 * Разрешает все модели в очереди и возвращает итоговую карту запечённых моделей.
	 * Модели с циклическими зависимостями исключаются с предупреждением.
	 *
	 * @return иммутабельная карта идентификатор → запечённая модель
	 */
	public Map<Identifier, BakedSimpleModel> collectModels() {
		List<ReferencedModelsCollector.Holder> list = new ArrayList<>();
		this.resolveAll(list);
		checkIfValid(list);
		Builder<Identifier, BakedSimpleModel> builder = ImmutableMap.builder();

		this.modelCache.forEach((id, model) -> {
			if (model.valid) {
				builder.put(id, model);
			}
			else {
				LOGGER.warn("Model {} ignored due to cyclic dependency", id);
			}
		});

		return builder.build();
	}

	/**
	 * Разрешает все модели из очереди, связывая их с родительскими держателями.
	 */
	private void resolveAll(List<ReferencedModelsCollector.Holder> models) {
		ReferencedModelsCollector.Holder pending;

		while ((pending = this.queue.poll()) != null) {
			Identifier identifier = Objects.requireNonNull(pending.model.parent());
			ReferencedModelsCollector.Holder parentHolder = this.resolve(identifier);
			pending.parent = parentHolder;

			if (parentHolder.valid) {
				pending.valid = true;
			}
			else {
				models.add(pending);
			}
		}
	}

	/**
	 * Итеративно помечает модели как валидные, если их родитель стал валидным.
	 * Повторяет до тех пор, пока не останется изменений.
	 */
	private static void checkIfValid(List<ReferencedModelsCollector.Holder> models) {
		boolean changed = true;

		while (changed) {
			changed = false;
			Iterator<ReferencedModelsCollector.Holder> iterator = models.iterator();

			while (iterator.hasNext()) {
				ReferencedModelsCollector.Holder entry = iterator.next();

				if (Objects.requireNonNull(entry.parent).valid) {
					entry.valid = true;
					iterator.remove();
					changed = true;
				}
			}
		}
	}

	/**
	 * Держатель незапечённой модели с ленивым вычислением свойств.
	 * Реализует {@link BakedSimpleModel} через атомарный массив свойств.
	 */
	@Environment(EnvType.CLIENT)
	static class Holder implements BakedSimpleModel {

		private static final ReferencedModelsCollector.Property<Boolean> AMBIENT_OCCLUSION_PROPERTY = createProperty(0);
		private static final ReferencedModelsCollector.Property<UnbakedModel.GuiLight>
				GUI_LIGHT_PROPERTY =
				createProperty(1);
		private static final ReferencedModelsCollector.Property<Geometry> GEOMETRY_PROPERTY = createProperty(2);
		private static final ReferencedModelsCollector.Property<ModelTransformation>
				TRANSFORMATIONS_PROPERTY =
				createProperty(3);
		private static final ReferencedModelsCollector.Property<ModelTextures> TEXTURE_PROPERTY = createProperty(4);
		private static final ReferencedModelsCollector.Property<Sprite> PARTICLE_TEXTURE_PROPERTY = createProperty(5);
		private static final ReferencedModelsCollector.Property<BakedGeometry>
				BAKED_GEOMETRY_PROPERTY =
				createProperty(6);
		private static final int PROPERTY_COUNT = 7;

		private final Identifier id;
		boolean valid;
		ReferencedModelsCollector.@Nullable Holder parent;
		final UnbakedModel model;
		private final AtomicReferenceArray<@Nullable Object> properties = new AtomicReferenceArray<>(7);
		private final Map<ModelBakeSettings, BakedGeometry> bakeCache = new ConcurrentHashMap<>();

		private static <T> ReferencedModelsCollector.Property<T> createProperty(int i) {
			Objects.checkIndex(i, 7);
			return new ReferencedModelsCollector.Property<>(i);
		}

		Holder(Identifier id, UnbakedModel model, boolean valid) {
			this.id = id;
			this.model = model;
			this.valid = valid;
		}

		@Override
		public UnbakedModel getModel() {
			return this.model;
		}

		@Override
		public @Nullable BakedSimpleModel getParent() {
			return this.parent;
		}

		@Override
		public String name() {
			return this.id.toString();
		}

		private <T> @Nullable T getProperty(ReferencedModelsCollector.Property<T> property) {
			return (T) this.properties.get(property.index);
		}

		private <T> T setProperty(ReferencedModelsCollector.Property<T> property, T value) {
			T object = (T) this.properties.compareAndExchange(property.index, null, value);
			return object == null ? value : object;
		}

		private <T> T getProperty(
				ReferencedModelsCollector.Property<T> property,
				Function<BakedSimpleModel, T> fallback
		) {
			T object = this.getProperty(property);
			return object != null ? object : this.setProperty(property, fallback.apply(this));
		}

		@Override
		public boolean getAmbientOcclusion() {
			return this.<Boolean>getProperty(
					AMBIENT_OCCLUSION_PROPERTY,
					model -> BakedSimpleModel.getAmbientOcclusion(model)
			);
		}

		@Override
		public UnbakedModel.GuiLight getGuiLight() {
			return this.getProperty(GUI_LIGHT_PROPERTY, model -> BakedSimpleModel.getGuiLight(model));
		}

		@Override
		public ModelTransformation getTransformations() {
			return this.getProperty(TRANSFORMATIONS_PROPERTY, BakedSimpleModel::copyTransformations);
		}

		@Override
		public Geometry getGeometry() {
			return this.getProperty(GEOMETRY_PROPERTY, model -> BakedSimpleModel.getGeometry(model));
		}

		@Override
		public ModelTextures getTextures() {
			return this.getProperty(TEXTURE_PROPERTY, model -> BakedSimpleModel.getTextures(model));
		}

		@Override
		public Sprite getParticleTexture(ModelTextures textures, Baker baker) {
			Sprite sprite = this.getProperty(PARTICLE_TEXTURE_PROPERTY);
			return sprite != null
			       ? sprite
			       : this.setProperty(
					       PARTICLE_TEXTURE_PROPERTY,
					       BakedSimpleModel.getParticleTexture(textures, baker, this)
			       );
		}

		private BakedGeometry getBakedGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings) {
			BakedGeometry bakedGeometry = this.getProperty(BAKED_GEOMETRY_PROPERTY);
			return bakedGeometry != null
			       ? bakedGeometry
			       : this.setProperty(
					       BAKED_GEOMETRY_PROPERTY,
					       this.getGeometry().bake(textures, baker, settings, this)
			       );
		}

		@Override
		public BakedGeometry bakeGeometry(ModelTextures textures, Baker baker, ModelBakeSettings settings) {
			return settings == ModelRotation.IDENTITY
			       ? this.getBakedGeometry(textures, baker, settings)
			       : this.bakeCache.computeIfAbsent(
					       settings, settings1 -> {
						       Geometry geometry = this.getGeometry();
						       return geometry.bake(textures, baker, settings1, this);
					       }
			       );
		}
	}

	/**
	 * Типизированный индекс свойства в атомарном массиве держателя.
	 *
	 * @param <T>   тип значения свойства
	 * @param index индекс в массиве свойств
	 */
	@Environment(EnvType.CLIENT)
	record Property<T>(int index) {
	}
}
