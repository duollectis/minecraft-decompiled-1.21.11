package net.minecraft.dialog.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.dialog.action.DialogAction;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Базовый интерфейс для всех типов диалогов в игре.
 *
 * <p>Диалог — это интерактивное окно, отображаемое игроку сервером.
 * Каждый конкретный тип диалога регистрируется в {@link net.minecraft.registry.Registries#DIALOG_TYPE}
 * и сериализуется через диспетчерный {@link #CODEC}.</p>
 */
public interface Dialog {

	/** Кодек для ширины диалога: допустимый диапазон от 1 до 1024 пикселей. */
	Codec<Integer> WIDTH_CODEC = Codecs.rangedInt(1, 1024);

	/** Диспетчерный кодек, определяющий конкретный тип диалога по его {@link MapCodec}. */
	Codec<Dialog> CODEC = Registries.DIALOG_TYPE.getCodec().dispatch(Dialog::getCodec, mapCodec -> mapCodec);

	/** Кодек для одиночной записи реестра диалогов. */
	Codec<RegistryEntry<Dialog>> ENTRY_CODEC = RegistryElementCodec.of(RegistryKeys.DIALOG, CODEC);

	/** Кодек для списка записей реестра диалогов. */
	Codec<RegistryEntryList<Dialog>> ENTRY_LIST_CODEC = RegistryCodecs.entryList(RegistryKeys.DIALOG, CODEC);

	/** Пакетный кодек для передачи одиночной записи реестра диалогов по сети. */
	PacketCodec<RegistryByteBuf, RegistryEntry<Dialog>> ENTRY_PACKET_CODEC = PacketCodecs.registryEntry(
		RegistryKeys.DIALOG, PacketCodecs.unlimitedRegistryCodec(CODEC)
	);

	/** Пакетный кодек для передачи диалога по сети без ограничений размера. */
	PacketCodec<ByteBuf, Dialog> PACKET_CODEC = PacketCodecs.unlimitedCodec(CODEC);

	DialogCommonData common();

	MapCodec<? extends Dialog> getCodec();

	Optional<DialogAction> getCancelAction();
}
