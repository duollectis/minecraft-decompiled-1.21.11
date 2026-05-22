package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.dialog.DialogBodyHandlers;
import net.minecraft.client.gui.screen.dialog.DialogScreens;
import net.minecraft.client.gui.screen.dialog.InputControlHandlers;
import net.minecraft.client.render.item.model.ItemModelTypes;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.client.render.item.property.select.SelectProperties;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.client.texture.atlas.AtlasSourceManager;

/**
 * Точка входа для регистрации всех клиентских типов и обработчиков.
 * Гарантирует однократную инициализацию через volatile-флаг.
 */
@Environment(EnvType.CLIENT)
public class ClientBootstrap {

	private static volatile boolean initialized;

	/**
	 * Регистрирует все клиентские типы: модели предметов, источники тинта,
	 * свойства предметов, атласы текстур и обработчики диалогов.
	 * Идемпотентен — повторный вызов не имеет эффекта.
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		ItemModelTypes.bootstrap();
		SpecialModelTypes.bootstrap();
		TintSourceTypes.bootstrap();
		SelectProperties.bootstrap();
		BooleanProperties.bootstrap();
		NumericProperties.bootstrap();
		AtlasSourceManager.bootstrap();
		DialogScreens.bootstrap();
		InputControlHandlers.bootstrap();
		DialogBodyHandlers.bootstrap();
	}
}
