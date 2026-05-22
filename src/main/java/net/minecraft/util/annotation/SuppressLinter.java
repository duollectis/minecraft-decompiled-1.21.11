package net.minecraft.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Подавляет предупреждения статического анализатора (линтера) для аннотированного элемента.
 * Обязательно указывать причину подавления через {@link #reason()}.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface SuppressLinter {

	String reason();
}
