package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.collection.CollectionPredicate;
import net.minecraft.predicate.component.ComponentSubPredicate;

import java.util.Optional;

/**
 * Предикат для проверки содержимого сумки (bundle).
 */
public record BundleContentsPredicate(
		Optional<CollectionPredicate<ItemStack, ItemPredicate>> items
) implements ComponentSubPredicate<BundleContentsComponent> {

	public static final Codec<BundleContentsPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(
							CollectionPredicate.createCodec(ItemPredicate.CODEC)
									.optionalFieldOf("items")
									.forGetter(BundleContentsPredicate::items)
					)
					.apply(instance, BundleContentsPredicate::new)
	);

	@Override
	public ComponentType<BundleContentsComponent> getComponentType() {
		return DataComponentTypes.BUNDLE_CONTENTS;
	}

	public boolean test(BundleContentsComponent component) {
		return items.isEmpty() || items.get().test(component.iterate());
	}
}
