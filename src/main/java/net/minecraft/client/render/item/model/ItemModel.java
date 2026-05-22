package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.ContextSwapper;
import net.minecraft.util.HeldItemContext;
import org.jspecify.annotations.Nullable;

/**
 * Базовый интерфейс модели предмета на стороне клиента.
 * Отвечает за обновление состояния рендера {@link ItemRenderState} на основе
 * текущего стека предмета, контекста отображения и мирового состояния.
 * Реализации могут диспетчеризировать рендер по числовым свойствам,
 * перечислимым значениям или делегировать специализированным рендерерам.
 */
@Environment(EnvType.CLIENT)
public interface ItemModel {

	/**
	 * Обновляет состояние рендера предмета для текущего кадра.
	 *
	 * @param state          изменяемое состояние рендера, в которое записываются слои и ключи
	 * @param stack          стек предмета, для которого строится модель
	 * @param resolver       менеджер моделей предметов для разрешения вложенных моделей
	 * @param displayContext контекст отображения (рука, GUI, земля и т.д.)
	 * @param world          клиентский мир; может быть {@code null} вне игры
	 * @param heldItemContext контекст держателя предмета; может быть {@code null}
	 * @param seed           случайное зерно для выбора вариантов модели
	 */
	void update(
			ItemRenderState state,
			ItemStack stack,
			ItemModelManager resolver,
			ItemDisplayContext displayContext,
			@Nullable ClientWorld world,
			@Nullable HeldItemContext heldItemContext,
			int seed
	);

	/**
	 * Контекст запекания модели предмета, содержащий все необходимые зависимости
	 * для преобразования несериализованной модели в готовую к рендеру форму.
	 */
	@Environment(EnvType.CLIENT)
	record BakeContext(
			Baker blockModelBaker,
			LoadedEntityModels entityModelSet,
			SpriteHolder spriteHolder,
			PlayerSkinCache playerSkinRenderCache,
			ItemModel missingItemModel,
			@Nullable ContextSwapper contextSwapper
	) implements SpecialModelRenderer.BakeContext {
	}

	/**
	 * Несериализованная форма модели предмета, способная разрешать зависимости
	 * и запекаться в готовую {@link ItemModel}.
	 */
	@Environment(EnvType.CLIENT)
	interface Unbaked extends ResolvableModel {

		MapCodec<? extends ItemModel.Unbaked> getCodec();

		ItemModel bake(ItemModel.BakeContext context);
	}
}
