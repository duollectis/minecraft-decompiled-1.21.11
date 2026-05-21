package net.minecraft.obfuscate;

import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@TypeQualifierDefault({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
/**
 * {@code DontObfuscate}.
 */
public @interface DontObfuscate {
}
