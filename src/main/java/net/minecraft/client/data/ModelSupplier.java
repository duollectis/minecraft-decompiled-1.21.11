package net.minecraft.client.data;

import com.google.gson.JsonElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelSupplier}.
 */
public interface ModelSupplier extends Supplier<JsonElement> {
}
