package snownee.researchtable.plugin.minecraft;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.ConditionType;
import snownee.researchtable.core.ConditionTypes;
import snownee.researchtable.core.ICondition;

/**
 * Player-XP cost paid in during research. Behaves like {@link snownee.researchtable.plugin.forge.ConditionForgeEnergy}:
 * the goal is a flat point-count, and the table drains XP from the player on Submit until the goal is met.
 */
public class ConditionExperience implements ICondition<Integer> {
	private final int count;

	public ConditionExperience(int count) {
		this.count = count;
	}

	@Override
	public long matches(Integer e) {
		return e;
	}

	@Override
	public long getGoal() {
		return count;
	}

	@Override
	public Supplier<Class<Integer>> getMatchType() {
		return ConditionTypes.EXPERIENCE;
	}

	public static final ConditionType<ConditionExperience> TYPE = ConditionType.register(
			ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "experience"),
			(buf, c) -> buf.writeVarInt(c.count),
			buf -> new ConditionExperience(buf.readVarInt()));

	@Override
	public ConditionType<ConditionExperience> getType() {
		return TYPE;
	}
}
