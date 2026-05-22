package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Событие, срабатывающее при клике на текст в чате или интерфейсе.
 * Каждая реализация соответствует конкретному типу действия (открыть URL, выполнить команду и т.д.).
 */
public interface ClickEvent {

	Codec<ClickEvent> CODEC = Action.CODEC.dispatch("action", ClickEvent::getAction, action -> action.codec);

	Action getAction();

	/**
	 * Перечисление всех поддерживаемых типов кликовых событий.
	 * Флаг {@code userDefinable} определяет, может ли действие быть задано игроком в чате.
	 */
	enum Action implements StringIdentifiable {
		OPEN_URL("open_url", true, OpenUrl.CODEC),
		OPEN_FILE("open_file", false, OpenFile.CODEC),
		RUN_COMMAND("run_command", true, RunCommand.CODEC),
		SUGGEST_COMMAND("suggest_command", true, SuggestCommand.CODEC),
		SHOW_DIALOG("show_dialog", true, ShowDialog.CODEC),
		CHANGE_PAGE("change_page", true, ChangePage.CODEC),
		COPY_TO_CLIPBOARD("copy_to_clipboard", true, CopyToClipboard.CODEC),
		CUSTOM("custom", true, Custom.CODEC);

		public static final Codec<Action> UNVALIDATED_CODEC = StringIdentifiable.createCodec(Action::values);
		public static final Codec<Action> CODEC = UNVALIDATED_CODEC.validate(Action::validate);

		private final String name;
		private final boolean userDefinable;
		final MapCodec<? extends ClickEvent> codec;

		Action(String name, boolean userDefinable, MapCodec<? extends ClickEvent> codec) {
			this.name = name;
			this.userDefinable = userDefinable;
			this.codec = codec;
		}

		public boolean isUserDefinable() {
			return userDefinable;
		}

		@Override
		public String asString() {
			return name;
		}

		public MapCodec<? extends ClickEvent> getCodec() {
			return codec;
		}

		/**
		 * Проверяет, разрешено ли данное действие для использования игроком.
		 * Действия с {@code userDefinable = false} (например, {@code OPEN_FILE}) запрещены в пользовательском вводе.
		 */
		public static DataResult<Action> validate(Action action) {
			return action.isUserDefinable()
				? DataResult.success(action, Lifecycle.stable())
				: DataResult.error(() -> "Click event type not allowed: " + action);
		}
	}

	/**
	 * Переключает страницу книги на указанный номер.
	 */
	record ChangePage(int page) implements ClickEvent {

		public static final MapCodec<ChangePage> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.POSITIVE_INT.fieldOf("page").forGetter(ChangePage::page))
				.apply(instance, ChangePage::new)
		);

		@Override
		public Action getAction() {
			return Action.CHANGE_PAGE;
		}
	}

	/**
	 * Копирует строку в буфер обмена.
	 */
	record CopyToClipboard(String value) implements ClickEvent {

		public static final MapCodec<CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codec.STRING.fieldOf("value").forGetter(CopyToClipboard::value))
				.apply(instance, CopyToClipboard::new)
		);

		@Override
		public Action getAction() {
			return Action.COPY_TO_CLIPBOARD;
		}
	}

	/**
	 * Пользовательское кликовое событие с произвольным идентификатором и опциональной NBT-нагрузкой.
	 */
	record Custom(Identifier id, Optional<NbtElement> payload) implements ClickEvent {

		public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Identifier.CODEC.fieldOf("id").forGetter(Custom::id),
				Codec.PASSTHROUGH
					.<NbtElement>xmap(
						dynamic -> (NbtElement) dynamic.convert(NbtOps.INSTANCE).getValue(),
						element -> new com.mojang.serialization.Dynamic<>(NbtOps.INSTANCE, element)
					)
					.optionalFieldOf("payload")
					.forGetter(Custom::payload)
			).apply(instance, Custom::new)
		);

		@Override
		public Action getAction() {
			return Action.CUSTOM;
		}
	}

	/**
	 * Открывает локальный файл по указанному пути.
	 * Доступно только клиентским модам — сервер не может задать это действие игроку.
	 */
	record OpenFile(String path) implements ClickEvent {

		public static final MapCodec<OpenFile> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codec.STRING.fieldOf("path").forGetter(OpenFile::path))
				.apply(instance, OpenFile::new)
		);

		public OpenFile(File file) {
			this(file.toString());
		}

		public OpenFile(Path path) {
			this(path.toFile());
		}

		public File file() {
			return new File(path);
		}

		@Override
		public Action getAction() {
			return Action.OPEN_FILE;
		}
	}

	/**
	 * Открывает URL в браузере по умолчанию.
	 */
	record OpenUrl(URI uri) implements ClickEvent {

		public static final MapCodec<OpenUrl> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.URI.fieldOf("url").forGetter(OpenUrl::uri))
				.apply(instance, OpenUrl::new)
		);

		@Override
		public Action getAction() {
			return Action.OPEN_URL;
		}
	}

	/**
	 * Выполняет команду от имени игрока, кликнувшего на текст.
	 */
	record RunCommand(String command) implements ClickEvent {

		public static final MapCodec<RunCommand> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.CHAT_TEXT.fieldOf("command").forGetter(RunCommand::command))
				.apply(instance, RunCommand::new)
		);

		@Override
		public Action getAction() {
			return Action.RUN_COMMAND;
		}
	}

	/**
	 * Открывает диалоговое окно, связанное с указанной записью реестра диалогов.
	 */
	record ShowDialog(RegistryEntry<Dialog> dialog) implements ClickEvent {

		public static final MapCodec<ShowDialog> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Dialog.ENTRY_CODEC.fieldOf("dialog").forGetter(ShowDialog::dialog))
				.apply(instance, ShowDialog::new)
		);

		@Override
		public Action getAction() {
			return Action.SHOW_DIALOG;
		}
	}

	/**
	 * Вставляет команду в строку ввода чата без немедленного выполнения.
	 */
	record SuggestCommand(String command) implements ClickEvent {

		public static final MapCodec<SuggestCommand> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.CHAT_TEXT.fieldOf("command").forGetter(SuggestCommand::command))
				.apply(instance, SuggestCommand::new)
		);

		@Override
		public Action getAction() {
			return Action.SUGGEST_COMMAND;
		}
	}
}
