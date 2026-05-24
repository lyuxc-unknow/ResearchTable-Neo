package snownee.researchtable.plugin.forge;

import java.util.function.Supplier;

import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.ICondition;

public class ConditionForgeEnergy implements ICondition<Long> {
	private final long count;

	public ConditionForgeEnergy(long count) {
		this.count = count;
	}

	@Override
	public long matches(Long e) {
		return e;
	}

	@Override
	public long getGoal() {
		return count;
	}

	@Override
	public Supplier<Class<Long>> getMatchType() {
		return ConditionTypes.ENERGY;
	}

	public static final ConditionType<ConditionForgeEnergy> TYPE = ConditionType.register(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "forge_energy"),
			(buf, c) -> buf.writeVarLong(c.count),
			buf -> new ConditionForgeEnergy(buf.readVarLong()));

	@Override
	public ConditionType<ConditionForgeEnergy> getType() {
		return TYPE;
	}
}
