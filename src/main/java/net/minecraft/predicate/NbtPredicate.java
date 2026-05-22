package net.minecraft.predicate;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Предикат для проверки NBT-данных сущности, предмета или блока.
 * Использует частичное совпадение: проверяемый NBT должен содержать все ключи из {@link #nbt}.
 */
public record NbtPredicate(NbtCompound nbt) {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<NbtPredicate> CODEC =
			StringNbtReader.NBT_COMPOUND_CODEC.xmap(NbtPredicate::new, NbtPredicate::nbt);
	public static final PacketCodec<ByteBuf, NbtPredicate> PACKET_CODEC =
			PacketCodecs.NBT_COMPOUND.xmap(NbtPredicate::new, NbtPredicate::nbt);
	public static final String SELECTED_ITEM_KEY = "SelectedItem";

	public boolean test(ComponentsAccess components) {
		NbtComponent nbtComponent = components.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		return nbtComponent.matches(nbt);
	}

	public boolean test(Entity entity) {
		return test(entityToNbt(entity));
	}

	public boolean test(@Nullable NbtElement element) {
		return element != null && NbtHelper.matches(nbt, element, true);
	}

	/**
	 * Сериализует сущность в NBT для последующей проверки предикатом.
	 * Для игроков дополнительно добавляет выбранный предмет под ключом {@value #SELECTED_ITEM_KEY}.
	 *
	 * @param entity сущность для сериализации
	 * @return NBT-представление сущности
	 */
	public static NbtCompound entityToNbt(Entity entity) {
		NbtCompound result;

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
			entity.writeData(nbtWriteView);

			if (entity instanceof PlayerEntity playerEntity) {
				ItemStack selectedStack = playerEntity.getInventory().getSelectedStack();

				if (!selectedStack.isEmpty()) {
					nbtWriteView.put(SELECTED_ITEM_KEY, ItemStack.CODEC, selectedStack);
				}
			}

			result = nbtWriteView.getNbt();
		}

		return result;
	}
}
