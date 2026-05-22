package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/**
 * Модель, способная декларировать свои зависимости от других моделей перед запеканием.
 * Вызов {@link #resolve(Resolver)} позволяет системе загрузки заранее собрать
 * полный граф зависимостей и гарантировать их наличие к моменту запекания.
 */
@Environment(EnvType.CLIENT)
public interface ResolvableModel {

	void resolve(ResolvableModel.Resolver resolver);

	/**
	 * Коллектор зависимостей: принимает идентификаторы моделей,
	 * от которых зависит текущая модель.
	 */
	@Environment(EnvType.CLIENT)
	interface Resolver {

		void markDependency(Identifier id);
	}
}
