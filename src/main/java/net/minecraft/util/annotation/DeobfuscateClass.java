package net.minecraft.util.annotation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Указывает, что класс должен быть деобфусцирован в клиентских сборках.
 * Используется инструментами сборки для сохранения читаемых имён в стек-трейсах.
 */
@Retention(RetentionPolicy.CLASS)
@Environment(EnvType.CLIENT)
public @interface DeobfuscateClass {
}
