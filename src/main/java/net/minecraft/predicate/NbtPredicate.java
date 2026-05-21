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
 * {@code NbtPredicate}.
 */
public record NbtPredicate(NbtCompound nbt) {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<NbtPredicate>
			CODEC =
			StringNbtReader.NBT_COMPOUND_CODEC.xmap(NbtPredicate::new, NbtPredicate::nbt);
	public static final PacketCodec<ByteBuf, NbtPredicate>
			PACKET_CODEC =
			PacketCodecs.NBT_COMPOUND.xmap(NbtPredicate::new, NbtPredicate::nbt);
	public static final String SELECTED_ITEM_KEY = "SelectedItem";

	public boolean test(ComponentsAccess components) {
		NbtComponent nbtComponent = components.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		return nbtComponent.matches(this.nbt);
	}

	public boolean test(Entity entity) {
		return this.test(entityToNbt(entity));
	}

	public boolean test(@Nullable NbtElement element) {
		return element != null && NbtHelper.matches(this.nbt, element, true);
	}

	public static NbtCompound entityToNbt(Entity entity) {
		NbtCompound var7;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
			entity.writeData(nbtWriteView);
			if (entity instanceof PlayerEntity playerEntity) {
				ItemStack itemStack = playerEntity.getInventory().getSelectedStack();
				if (!itemStack.isEmpty()) {
					nbtWriteView.put("SelectedItem", ItemStack.CODEC, itemStack);
				}
			}

			var7 = nbtWriteView.getNbt();
		}

		return var7;
	}
}
