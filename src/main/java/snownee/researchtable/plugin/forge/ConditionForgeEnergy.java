package snownee.researchtable.plugin.forge;

import java.util.function.Supplier;

import snownee.researchtable.ResearchTable;
import snownee.researchtable.api.ICondition;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;

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
			ResearchTable.id("forge_energy"),
			(buf, c) -> buf.writeVarLong(c.count),
			buf -> new ConditionForgeEnergy(buf.readVarLong()));

	@Override
	public ConditionType<ConditionForgeEnergy> getType() {
		return TYPE;
	}
}
