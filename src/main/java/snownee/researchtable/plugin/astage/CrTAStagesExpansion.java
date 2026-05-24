package snownee.researchtable.plugin.astage;

import java.util.Set;

import javax.annotation.Nonnull;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.google.common.collect.ImmutableSet;

import snownee.researchtable.plugin.crafttweaker.ResearchBuilder;

/**
 * CrT-side hook so scripts can attach AStages criteria / rewards to a research:
 *   builder.setRequiredStages("nether", "end");
 *   builder.setRewardStages("unlocked_thing");
 * Implemented as a ZenScript Expansion so the AStages methods only appear in the {@code
 * ResearchBuilder} type when both CrT and AStages are present — keeping AStages an optional
 * dependency from the perspective of the core builder class.
 */
@ZenRegister
@ZenCodeType.Expansion("mods.researchtable.ResearchBuilder")
public class CrTAStagesExpansion {

	@ZenCodeType.Method
	public static ResearchBuilder setRewardStages(ResearchBuilder builder, @Nonnull String... stages) {
		builder.rewards.add(new RewardUnlockStages(stages));
		return builder;
	}

	@ZenCodeType.Method
	public static ResearchBuilder setTriggerStages(ResearchBuilder builder, @Nonnull String... stages) {
		builder.triggers.add(new RewardUnlockStages(stages));
		return builder;
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRequiredStages(ResearchBuilder builder, @Nonnull String... stages) {
		Set<String> set = ImmutableSet.copyOf(stages);
		builder.criteria.add(new CriterionStages(set, set.size()));
		return builder;
	}

	@ZenCodeType.Method
	public static ResearchBuilder setOptionalStages(ResearchBuilder builder, int amount, @Nonnull String... stages) {
		Set<String> set = ImmutableSet.copyOf(stages);
		builder.criteria.add(new CriterionStages(set, amount));
		return builder;
	}
}
