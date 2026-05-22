package net.minecraft.client.render.item.property.select;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Свойство выбора для системы предметных моделей ({@code select} модели).
 * Возвращает значение типа {@code T}, по которому выбирается нужная ветка модели.
 */
@Environment(EnvType.CLIENT)
public interface SelectProperty<T> {

	@Nullable T getValue(
			ItemStack stack,
			@Nullable ClientWorld world,
			@Nullable LivingEntity user,
			int seed,
			ItemDisplayContext displayContext
	);

	Codec<T> valueCodec();

	SelectProperty.Type<? extends SelectProperty<T>, T> getType();

	/**
	 * Метаданные типа свойства выбора: содержит кодек для десериализации {@code UnbakedSwitch}.
	 */
	@Environment(EnvType.CLIENT)
	record Type<P extends SelectProperty<T>, T>(MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {

		public static <P extends SelectProperty<T>, T> SelectProperty.Type<P, T> create(
				MapCodec<P> propertyCodec,
				Codec<T> valueCodec
		) {
			MapCodec<SelectItemModel.UnbakedSwitch<P, T>> mapCodec = RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							                    propertyCodec.forGetter(SelectItemModel.UnbakedSwitch::property),
							                    createCaseListCodec(valueCodec).forGetter(SelectItemModel.UnbakedSwitch::cases)
					                    )
					                    .apply(instance, SelectItemModel.UnbakedSwitch::new)
			);
			return new SelectProperty.Type<>(mapCodec);
		}

		public static <T> MapCodec<List<SelectItemModel.SwitchCase<T>>> createCaseListCodec(Codec<T> conditionCodec) {
			return SelectItemModel.SwitchCase
					.createCodec(conditionCodec)
					.listOf()
					.validate(SelectProperty.Type::validateCases)
					.fieldOf("cases");
		}

		private static <T> DataResult<List<SelectItemModel.SwitchCase<T>>> validateCases(List<SelectItemModel.SwitchCase<T>> cases) {
			if (cases.isEmpty()) {
				return DataResult.error(() -> "Empty case list");
			}
			else {
				Multiset<T> multiset = HashMultiset.create();

				for (SelectItemModel.SwitchCase<T> switchCase : cases) {
					multiset.addAll(switchCase.values());
				}

				return multiset.size() != multiset.entrySet().size()
				       ? DataResult.error(
						() -> "Duplicate case conditions: "
						      + multiset.entrySet()
						                .stream()
						                .filter(entry -> entry.getCount() > 1)
						                .map(entry -> entry.getElement().toString())
						                .collect(Collectors.joining(", "))
				)
				       : DataResult.success(cases);
			}
		}
	}
}
