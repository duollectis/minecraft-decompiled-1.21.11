package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.collection.ListOperation;

import java.util.List;

/**
 * Функция лута, устанавливающая или изменяющая страницы записной книжки.
 * Максимальное количество страниц ограничено 100.
 */
public class SetWritableBookPagesLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetWritableBookPagesLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					WritableBookContentComponent.PAGES_CODEC
						.fieldOf("pages")
						.forGetter(function -> function.pages),
					ListOperation.createCodec(100).forGetter(function -> function.operation)
				)
			)
			.apply(instance, SetWritableBookPagesLootFunction::new)
	);

	private final List<RawFilteredPair<String>> pages;
	private final ListOperation operation;

	protected SetWritableBookPagesLootFunction(
		List<LootCondition> conditions,
		List<RawFilteredPair<String>> pages,
		ListOperation operation
	) {
		super(conditions);
		this.pages = pages;
		this.operation = operation;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(DataComponentTypes.WRITABLE_BOOK_CONTENT, WritableBookContentComponent.DEFAULT, this::applyToBook);
		return stack;
	}

	public WritableBookContentComponent applyToBook(WritableBookContentComponent current) {
		List<RawFilteredPair<String>> newPages = operation.apply(current.pages(), pages, 100);
		return current.withPages(newPages);
	}

	@Override
	public LootFunctionType<SetWritableBookPagesLootFunction> getType() {
		return LootFunctionTypes.SET_WRITABLE_BOOK_PAGES;
	}
}
