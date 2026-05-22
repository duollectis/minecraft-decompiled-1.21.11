package net.minecraft.client.data;

import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Supplier;

/**
 * Поставщик JSON-элемента модели. Функциональный интерфейс, расширяющий
 * {@link Supplier}, используется как ленивый генератор JSON-содержимого модели.
 */
@Environment(EnvType.CLIENT)
public interface ModelSupplier extends Supplier<JsonElement> {
}
