package net.minecraft.loot.provider.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.List;

/**
 * Провайдер числа, читающий значение из командного хранилища данных по NBT-пути.
 * Если путь не найден или значение не является числом, возвращает значение по умолчанию.
 */
public record StorageLootNumberProvider(
	Identifier storage,
	NbtPathArgumentType.NbtPath path
) implements LootNumberProvider {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<StorageLootNumberProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Identifier.CODEC.fieldOf("storage").forGetter(StorageLootNumberProvider::storage),
			NbtPathArgumentType.NbtPath.CODEC.fieldOf("path").forGetter(StorageLootNumberProvider::path)
		)
		.apply(instance, StorageLootNumberProvider::new)
	);

	@Override
	public LootNumberProviderType getType() {
		return LootNumberProviderTypes.STORAGE;
	}

	private Number readNumber(LootContext context, Number fallback) {
		NbtCompound storageNbt = context.getWorld().getServer().getDataCommandStorage().get(storage);
		try {
			List<NbtElement> found = path.get(storageNbt);
			if (found.size() == 1 && found.getFirst() instanceof AbstractNbtNumber nbtNumber) {
				return nbtNumber.numberValue();
			}
		} catch (CommandSyntaxException exception) {
			LOGGER.warn("Failed to read number from storage '{}' at path '{}'", storage, path, exception);
		}
		return fallback;
	}

	@Override
	public float nextFloat(LootContext context) {
		return readNumber(context, 0.0F).floatValue();
	}

	@Override
	public int nextInt(LootContext context) {
		return readNumber(context, 0).intValue();
	}
}
