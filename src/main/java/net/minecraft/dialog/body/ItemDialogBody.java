package net.minecraft.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Элемент тела диалога, отображающий предмет с опциональным описанием.
 *
 * @param item            предмет для отображения
 * @param description     опциональное текстовое описание под предметом
 * @param showDecorations отображать ли декорации предмета (зачарования, прочность и т.д.)
 * @param showTooltip     отображать ли подсказку при наведении
 * @param width           ширина иконки предмета в пикселях (1–256)
 * @param height          высота иконки предмета в пикселях (1–256)
 */
public record ItemDialogBody(
	ItemStack item,
	Optional<PlainMessageDialogBody> description,
	boolean showDecorations,
	boolean showTooltip,
	int width,
	int height
) implements DialogBody {

	public static final MapCodec<ItemDialogBody> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			ItemStack.VALIDATED_CODEC.fieldOf("item").forGetter(ItemDialogBody::item),
			PlainMessageDialogBody.ALTERNATIVE_CODEC.optionalFieldOf("description").forGetter(ItemDialogBody::description),
			Codec.BOOL.optionalFieldOf("show_decorations", true).forGetter(ItemDialogBody::showDecorations),
			Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(ItemDialogBody::showTooltip),
			Codecs.rangedInt(1, 256).optionalFieldOf("width", 16).forGetter(ItemDialogBody::width),
			Codecs.rangedInt(1, 256).optionalFieldOf("height", 16).forGetter(ItemDialogBody::height)
		).apply(instance, ItemDialogBody::new)
	);

	@Override
	public MapCodec<ItemDialogBody> getTypeCodec() {
		return CODEC;
	}
}
