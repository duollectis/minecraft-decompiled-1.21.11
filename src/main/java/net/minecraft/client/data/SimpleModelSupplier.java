package net.minecraft.client.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/**
 * Простейший поставщик модели, генерирующий JSON только с полем {@code parent}.
 * Используется для моделей-делегатов, которые полностью наследуют родительскую модель.
 */
@Environment(EnvType.CLIENT)
public class SimpleModelSupplier implements ModelSupplier {

	private final Identifier parent;

	public SimpleModelSupplier(Identifier parent) {
		this.parent = parent;
	}

	@Override
	public JsonElement get() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("parent", parent.toString());
		return jsonObject;
	}
}
