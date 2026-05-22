package net.minecraft.util.path;

import java.nio.file.Path;

/**
 * Запись об обнаруженном символьном пути: хранит путь к самой ссылке
 * и путь к её цели для последующей валидации или отчёта об ошибке.
 */
public record SymlinkEntry(Path link, Path target) {
}
