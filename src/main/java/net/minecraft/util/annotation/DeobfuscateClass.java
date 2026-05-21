package net.minecraft.util.annotation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//import com.google.firebase.crashlytics.buildtools.reloc.javax.annotation.meta.TypeQualifierDefault;

//@TypeQualifierDefault({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Environment(EnvType.CLIENT)
/**
 * {@code DeobfuscateClass}.
 */
public @interface DeobfuscateClass {
}
