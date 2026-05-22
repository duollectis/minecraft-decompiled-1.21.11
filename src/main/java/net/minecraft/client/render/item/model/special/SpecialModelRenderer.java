package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Базовый интерфейс специализированного рендерера предмета.
 * Используется для предметов с нетривиальной геометрией, которые не могут быть
 * представлены стандартной блочной моделью: щиты, головы, сундуки, знамёна и т.д.
 * Рендерер извлекает данные из стека предмета через {@link #getData} и использует
 * их при вызове {@link #render}.
 *
 * @param <T> тип данных, извлекаемых из стека предмета
 */
@Environment(EnvType.CLIENT)
public interface SpecialModelRenderer<T> {

	/**
	 * Рендерит специальную модель предмета.
	 *
	 * @param data           данные, извлечённые из стека предмета; может быть {@code null}
	 * @param displayContext контекст отображения предмета
	 * @param matrices       стек матриц трансформации
	 * @param queue          очередь команд рендера
	 * @param light          упакованное значение освещения
	 * @param overlay        упакованное значение оверлея
	 * @param glint          нужно ли рендерить эффект зачарования
	 * @param seed           случайное зерно для вариативности
	 */
	void render(
			@Nullable T data,
			ItemDisplayContext displayContext,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			boolean glint,
			int seed
	);

	/**
	 * Собирает все вершины модели для расчёта bounding box и других геометрических операций.
	 *
	 * @param consumer получатель вершин модели
	 */
	void collectVertices(Consumer<Vector3fc> consumer);

	/**
	 * Извлекает данные для рендера из стека предмета.
	 *
	 * @param stack стек предмета
	 * @return данные для рендера или {@code null}, если данные отсутствуют
	 */
	@Nullable T getData(ItemStack stack);

	/**
	 * Контекст запекания специальных рендереров, предоставляющий доступ
	 * к загруженным моделям сущностей, атласу спрайтов и кэшу скинов игроков.
	 */
	@Environment(EnvType.CLIENT)
	interface BakeContext {

		LoadedEntityModels entityModelSet();

		SpriteHolder spriteHolder();

		PlayerSkinCache playerSkinRenderCache();

		/**
		 * Простая реализация контекста запекания на основе record.
		 */
		@Environment(EnvType.CLIENT)
		record Simple(
				LoadedEntityModels entityModelSet,
				SpriteHolder spriteHolder,
				PlayerSkinCache playerSkinRenderCache
		) implements SpecialModelRenderer.BakeContext {
		}
	}

	/**
	 * Несериализованная форма специального рендерера, способная запекаться
	 * в готовый {@link SpecialModelRenderer}.
	 */
	@Environment(EnvType.CLIENT)
	interface Unbaked {

		@Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context);

		MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec();
	}
}
