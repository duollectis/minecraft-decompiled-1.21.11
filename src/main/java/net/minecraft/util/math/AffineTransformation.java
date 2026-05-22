package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Аффинное преобразование в 3D-пространстве, хранящееся как матрица 4×4.
 * Компоненты (перемещение, вращение, масштаб) вычисляются лениво через SVD-разложение.
 */
public final class AffineTransformation {

	public static final Codec<AffineTransformation> CODEC =RecordCodecBuilder.create(instance -> instance.group(
		Codecs.VECTOR_3F.fieldOf("translation").forGetter(AffineTransformation::getTranslation),
		Codecs.ROTATION.fieldOf("left_rotation").forGetter(AffineTransformation::getLeftRotation),
		Codecs.VECTOR_3F.fieldOf("scale").forGetter(AffineTransformation::getScale),
		Codecs.ROTATION.fieldOf("right_rotation").forGetter(AffineTransformation::getRightRotation)
	).apply(instance, AffineTransformation::new));

	public static final Codec<AffineTransformation> ANY_CODEC = Codec.withAlternative(
			CODEC,
			Codecs.MATRIX_4F.xmap(AffineTransformation::new, AffineTransformation::getMatrix)
	);

	private static final AffineTransformation IDENTITY = Util.make(() -> {
		AffineTransformation t = new AffineTransformation(new Matrix4f());
		t.translation = new Vector3f();
		t.leftRotation = new Quaternionf();
		t.scale = new Vector3f(1.0F, 1.0F, 1.0F);
		t.rightRotation = new Quaternionf();
		t.initialized = true;
		return t;
	});

	private final Matrix4fc matrix;
	private boolean initialized;
	private @Nullable Vector3fc translation;
	private @Nullable Quaternionfc leftRotation;
	private @Nullable Vector3fc scale;
	private @Nullable Quaternionfc rightRotation;

	public AffineTransformation(@Nullable Matrix4fc matrix) {
		this.matrix = matrix == null ? new Matrix4f() : matrix;
	}

	public AffineTransformation(
			@Nullable Vector3fc translation,
			@Nullable Quaternionfc leftRotation,
			@Nullable Vector3fc scale,
			@Nullable Quaternionfc rightRotation
	) {
		matrix = buildMatrix(translation, leftRotation, scale, rightRotation);
		this.translation = translation != null ? translation : new Vector3f();
		this.leftRotation = leftRotation != null ? leftRotation : new Quaternionf();
		this.scale = scale != null ? scale : new Vector3f(1.0F, 1.0F, 1.0F);
		this.rightRotation = rightRotation != null ? rightRotation : new Quaternionf();
		initialized = true;
	}

	public static AffineTransformation identity() {
		return IDENTITY;
	}

	public AffineTransformation multiply(AffineTransformation other) {
		Matrix4f result = copyMatrix();
		result.mul(other.getMatrix());
		return new AffineTransformation(result);
	}

	public @Nullable AffineTransformation invert() {
		if (this == IDENTITY) {
			return this;
		}

		Matrix4f inverted = copyMatrix().invertAffine();
		return inverted.isFinite() ? new AffineTransformation(inverted) : null;
	}

	public AffineTransformation interpolate(AffineTransformation target, float factor) {
		return new AffineTransformation(
				getTranslation().lerp(target.getTranslation(), factor, new Vector3f()),
				getLeftRotation().slerp(target.getLeftRotation(), factor, new Quaternionf()),
				getScale().lerp(target.getScale(), factor, new Vector3f()),
				getRightRotation().slerp(target.getRightRotation(), factor, new Quaternionf())
		);
	}

	public Matrix4fc getMatrix() {
		return matrix;
	}

	public Matrix4f copyMatrix() {
		return new Matrix4f(matrix);
	}

	public Vector3fc getTranslation() {
		ensureInitialized();
		return translation;
	}

	public Quaternionfc getLeftRotation() {
		ensureInitialized();
		return leftRotation;
	}

	public Vector3fc getScale() {
		ensureInitialized();
		return scale;
	}

	public Quaternionfc getRightRotation() {
		ensureInitialized();
		return rightRotation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof AffineTransformation other && Objects.equals(matrix, other.matrix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(matrix);
	}

	/**
	 * Ленивая инициализация компонентов через SVD-разложение матрицы.
	 * Вызывается только при первом обращении к translation/rotation/scale.
	 */
	private void ensureInitialized() {
		if (initialized) {
			return;
		}

		float scale = 1.0F / matrix.m33();
		Triple<Quaternionf, Vector3f, Quaternionf> svd = MatrixUtil.svdDecompose(new Matrix3f(matrix).scale(scale));
		translation = matrix.getTranslation(new Vector3f()).mul(scale);
		leftRotation = new Quaternionf(svd.getLeft());
		this.scale = new Vector3f(svd.getMiddle());
		rightRotation = new Quaternionf(svd.getRight());
		initialized = true;
	}

	private static Matrix4f buildMatrix(
			@Nullable Vector3fc translation,
			@Nullable Quaternionfc leftRotation,
			@Nullable Vector3fc scale,
			@Nullable Quaternionfc rightRotation
	) {
		Matrix4f matrix = new Matrix4f();
		if (translation != null) {
			matrix.translation(translation);
		}
		if (leftRotation != null) {
			matrix.rotate(leftRotation);
		}
		if (scale != null) {
			matrix.scale(scale);
		}
		if (rightRotation != null) {
			matrix.rotate(rightRotation);
		}
		return matrix;
	}
}
