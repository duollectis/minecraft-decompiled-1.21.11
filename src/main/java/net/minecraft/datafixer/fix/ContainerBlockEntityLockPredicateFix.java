package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code ContainerBlockEntityLockPredicateFix}.
 */
public class ContainerBlockEntityLockPredicateFix extends DataFix {

	public ContainerBlockEntityLockPredicateFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return this.fixTypeEverywhereTyped(
				"ContainerBlockEntityLockPredicateFix",
				this.getInputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY),
				ContainerBlockEntityLockPredicateFix::fixLock
		);
	}

	private static Typed<?> fixLock(Typed<?> typed) {
		return typed.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.renameAndFixField("Lock", "lock", LockComponentPredicateFix::fixLock)
		);
	}
}
