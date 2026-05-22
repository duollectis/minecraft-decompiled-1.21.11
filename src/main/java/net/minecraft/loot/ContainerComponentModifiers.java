package net.minecraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Реестр стандартных модификаторов компонентов-контейнеров предметов.
 *
 * <p>Содержит реализации {@link ContainerComponentModifier} для трёх типов контейнеров:
 * обычного инвентаря ({@link #CONTAINER}), связки ({@link #BUNDLE_CONTENTS})
 * и заряженных снарядов ({@link #CHARGED_PROJECTILES}).</p>
 */
public interface ContainerComponentModifiers {

	ContainerComponentModifier<ContainerComponent> CONTAINER = new ContainerComponentModifier<>() {
		@Override
		public ComponentType<ContainerComponent> getComponentType() {
			return DataComponentTypes.CONTAINER;
		}

		@Override
		public ContainerComponent getDefault() {
			return ContainerComponent.DEFAULT;
		}

		@Override
		public Stream<ItemStack> stream(ContainerComponent component) {
			return component.stream();
		}

		@Override
		public ContainerComponent apply(ContainerComponent component, Stream<ItemStack> contents) {
			return ContainerComponent.fromStacks(contents.toList());
		}
	};

	ContainerComponentModifier<BundleContentsComponent> BUNDLE_CONTENTS = new ContainerComponentModifier<>() {
		@Override
		public ComponentType<BundleContentsComponent> getComponentType() {
			return DataComponentTypes.BUNDLE_CONTENTS;
		}

		@Override
		public BundleContentsComponent getDefault() {
			return BundleContentsComponent.DEFAULT;
		}

		@Override
		public Stream<ItemStack> stream(BundleContentsComponent component) {
			return component.stream();
		}

		@Override
		public BundleContentsComponent apply(BundleContentsComponent component, Stream<ItemStack> contents) {
			BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(component).clear();
			contents.forEach(builder::add);
			return builder.build();
		}
	};

	ContainerComponentModifier<ChargedProjectilesComponent> CHARGED_PROJECTILES = new ContainerComponentModifier<>() {
		@Override
		public ComponentType<ChargedProjectilesComponent> getComponentType() {
			return DataComponentTypes.CHARGED_PROJECTILES;
		}

		@Override
		public ChargedProjectilesComponent getDefault() {
			return ChargedProjectilesComponent.DEFAULT;
		}

		@Override
		public Stream<ItemStack> stream(ChargedProjectilesComponent component) {
			return component.getProjectiles().stream();
		}

		@Override
		public ChargedProjectilesComponent apply(ChargedProjectilesComponent component, Stream<ItemStack> contents) {
			return ChargedProjectilesComponent.of(contents.toList());
		}
	};

	Map<ComponentType<?>, ContainerComponentModifier<?>> TYPE_TO_MODIFIER = Stream
		.of(CONTAINER, BUNDLE_CONTENTS, CHARGED_PROJECTILES)
		.collect(
			Collectors.toMap(
				ContainerComponentModifier::getComponentType,
				modifier -> (ContainerComponentModifier<?>) modifier
			)
		);

	/**
	 * Кодек для сериализации модификатора по типу компонента из реестра.
	 * Возвращает ошибку, если тип компонента не имеет зарегистрированного модификатора.
	 */
	Codec<ContainerComponentModifier<?>> MODIFIER_CODEC = Registries.DATA_COMPONENT_TYPE.getCodec().comapFlatMap(
		componentType -> {
			ContainerComponentModifier<?> modifier = TYPE_TO_MODIFIER.get(componentType);
			return modifier != null
				? DataResult.success(modifier)
				: DataResult.error(() -> "No items in component");
		},
		ContainerComponentModifier::getComponentType
	);
}
