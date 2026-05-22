package net.minecraft.predicate.component;

import com.mojang.serialization.Codec;
import net.minecraft.predicate.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Реестр всех типов предикатов компонентов предметов.
 * Каждый тип регистрируется в {@link Registries#DATA_COMPONENT_PREDICATE_TYPE}.
 */
public class ComponentPredicateTypes {

	public static final ComponentPredicate.Type<DamagePredicate> DAMAGE =
			register("damage", DamagePredicate.CODEC);

	public static final ComponentPredicate.Type<EnchantmentsPredicate.Enchantments> ENCHANTMENTS =
			register("enchantments", EnchantmentsPredicate.Enchantments.CODEC);

	public static final ComponentPredicate.Type<EnchantmentsPredicate.StoredEnchantments> STORED_ENCHANTMENTS =
			register("stored_enchantments", EnchantmentsPredicate.StoredEnchantments.CODEC);

	public static final ComponentPredicate.Type<PotionContentsPredicate> POTION_CONTENTS =
			register("potion_contents", PotionContentsPredicate.CODEC);

	public static final ComponentPredicate.Type<CustomDataPredicate> CUSTOM_DATA =
			register("custom_data", CustomDataPredicate.CODEC);

	public static final ComponentPredicate.Type<ContainerPredicate> CONTAINER =
			register("container", ContainerPredicate.CODEC);

	public static final ComponentPredicate.Type<BundleContentsPredicate> BUNDLE_CONTENTS =
			register("bundle_contents", BundleContentsPredicate.CODEC);

	public static final ComponentPredicate.Type<FireworkExplosionPredicate> FIREWORK_EXPLOSION =
			register("firework_explosion", FireworkExplosionPredicate.CODEC);

	public static final ComponentPredicate.Type<FireworksPredicate> FIREWORKS =
			register("fireworks", FireworksPredicate.CODEC);

	public static final ComponentPredicate.Type<WritableBookContentPredicate> WRITABLE_BOOK_CONTENT =
			register("writable_book_content", WritableBookContentPredicate.CODEC);

	public static final ComponentPredicate.Type<WrittenBookContentPredicate> WRITTEN_BOOK_CONTENT =
			register("written_book_content", WrittenBookContentPredicate.CODEC);

	public static final ComponentPredicate.Type<AttributeModifiersPredicate> ATTRIBUTE_MODIFIERS =
			register("attribute_modifiers", AttributeModifiersPredicate.CODEC);

	public static final ComponentPredicate.Type<TrimPredicate> TRIM =
			register("trim", TrimPredicate.CODEC);

	public static final ComponentPredicate.Type<JukeboxPlayablePredicate> JUKEBOX_PLAYABLE =
			register("jukebox_playable", JukeboxPlayablePredicate.CODEC);

	private static <T extends ComponentPredicate> ComponentPredicate.Type<T> register(String id, Codec<T> codec) {
		return Registry.register(
				Registries.DATA_COMPONENT_PREDICATE_TYPE,
				id,
				new ComponentPredicate.OfValue<>(codec)
		);
	}

	public static ComponentPredicate.Type<?> getDefault(Registry<ComponentPredicate.Type<?>> registry) {
		return DAMAGE;
	}
}
