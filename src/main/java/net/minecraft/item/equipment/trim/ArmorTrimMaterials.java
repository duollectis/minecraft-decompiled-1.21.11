package net.minecraft.item.equipment.trim;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProvidesTrimMaterialComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Optional;

/**
 * {@code ArmorTrimMaterials}.
 */
public class ArmorTrimMaterials {

	public static final RegistryKey<ArmorTrimMaterial> QUARTZ = of("quartz");
	public static final RegistryKey<ArmorTrimMaterial> IRON = of("iron");
	public static final RegistryKey<ArmorTrimMaterial> NETHERITE = of("netherite");
	public static final RegistryKey<ArmorTrimMaterial> REDSTONE = of("redstone");
	public static final RegistryKey<ArmorTrimMaterial> COPPER = of("copper");
	public static final RegistryKey<ArmorTrimMaterial> GOLD = of("gold");
	public static final RegistryKey<ArmorTrimMaterial> EMERALD = of("emerald");
	public static final RegistryKey<ArmorTrimMaterial> DIAMOND = of("diamond");
	public static final RegistryKey<ArmorTrimMaterial> LAPIS = of("lapis");
	public static final RegistryKey<ArmorTrimMaterial> AMETHYST = of("amethyst");
	public static final RegistryKey<ArmorTrimMaterial> RESIN = of("resin");

	public static void bootstrap(Registerable<ArmorTrimMaterial> registry) {
		register(registry, QUARTZ, Style.EMPTY.withColor(14931140), ArmorTrimAssets.QUARTZ);
		register(registry, IRON, Style.EMPTY.withColor(15527148), ArmorTrimAssets.IRON);
		register(registry, NETHERITE, Style.EMPTY.withColor(6445145), ArmorTrimAssets.NETHERITE);
		register(registry, REDSTONE, Style.EMPTY.withColor(9901575), ArmorTrimAssets.REDSTONE);
		register(registry, COPPER, Style.EMPTY.withColor(11823181), ArmorTrimAssets.COPPER);
		register(registry, GOLD, Style.EMPTY.withColor(14594349), ArmorTrimAssets.GOLD);
		register(registry, EMERALD, Style.EMPTY.withColor(1155126), ArmorTrimAssets.EMERALD);
		register(registry, DIAMOND, Style.EMPTY.withColor(7269586), ArmorTrimAssets.DIAMOND);
		register(registry, LAPIS, Style.EMPTY.withColor(4288151), ArmorTrimAssets.LAPIS);
		register(registry, AMETHYST, Style.EMPTY.withColor(10116294), ArmorTrimAssets.AMETHYST);
		register(registry, RESIN, Style.EMPTY.withColor(16545810), ArmorTrimAssets.RESIN);
	}

	public static Optional<RegistryEntry<ArmorTrimMaterial>> get(
			RegistryWrapper.WrapperLookup registries,
			ItemStack stack
	) {
		ProvidesTrimMaterialComponent
				providesTrimMaterialComponent =
				stack.get(DataComponentTypes.PROVIDES_TRIM_MATERIAL);
		return providesTrimMaterialComponent != null ? providesTrimMaterialComponent.getMaterial(registries)
		                                             : Optional.empty();
	}

	private static void register(
			Registerable<ArmorTrimMaterial> registry,
			RegistryKey<ArmorTrimMaterial> key,
			Style style,
			ArmorTrimAssets assets
	) {
		Text text = Text.translatable(Util.createTranslationKey("trim_material", key.getValue())).fillStyle(style);
		registry.register(key, new ArmorTrimMaterial(assets, text));
	}

	private static RegistryKey<ArmorTrimMaterial> of(String id) {
		return RegistryKey.of(RegistryKeys.TRIM_MATERIAL, Identifier.ofVanilla(id));
	}
}
