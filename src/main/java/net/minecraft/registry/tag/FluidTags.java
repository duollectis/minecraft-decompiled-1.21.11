package net.minecraft.registry.tag;

import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Реестр тегов жидкостей ванильного Minecraft.
 * <p>
 * Теги используются для группировки жидкостей по категориям, что позволяет
 * проверять принадлежность жидкости к группе без перечисления конкретных типов.
 * Например, {@link #WATER} объединяет все варианты воды, {@link #LAVA} — лавы.
 */
public final class FluidTags {

	public static final TagKey<Fluid> WATER = of("water");
	public static final TagKey<Fluid> LAVA = of("lava");

	private FluidTags() {
	}

	private static TagKey<Fluid> of(String id) {
		return TagKey.of(RegistryKeys.FLUID, Identifier.ofVanilla(id));
	}
}
