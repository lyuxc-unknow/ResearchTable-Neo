package snownee.researchtable.plugin.minecraft;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;

import net.minecraft.resources.ResourceLocation;
import snownee.researchtable.plugin.crafttweaker.ResearchBuilder;

/**
 * CrT-side hook so scripts can attach vanilla-world criteria / rewards to a research:
 *   builder.setRequiredBiomes("minecraft:plains", "minecraft:forest");
 *   builder.setRequiredDimensions("minecraft:the_nether");
 *   builder.setRequiredWeather("rain");
 *   builder.setRequiredTime(13000, 23000);   // night
 *   builder.addXPCondition(100);             // submit-time XP cost (drained from player)
 *   builder.setRewardXP(100);                // experience points
 *   builder.setRewardXPLevels(5);
 * Implemented as a ZenScript Expansion so these methods are only added to the
 * {@code ResearchBuilder} type when CrT is loaded, keeping the core builder class free of CrT
 * imports.
 */
@ZenRegister
@ZenCodeType.Expansion("mods.researchtable.ResearchBuilder")
public class CrTMinecraftExpansion {

	@ZenCodeType.Method
	public static ResearchBuilder setRequiredBiomes(ResearchBuilder builder, @Nonnull String... biomes) {
		return builder.addCriterion(new CriterionBiome(parseIds(biomes)));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRequiredDimensions(ResearchBuilder builder, @Nonnull String... dimensions) {
		return builder.addCriterion(new CriterionDimension(parseIds(dimensions)));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRequiredWeather(ResearchBuilder builder, @Nonnull String weather) {
		CriterionWeather.Weather w;
		try {
			w = CriterionWeather.Weather.valueOf(weather.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown weather '" + weather + "', expected one of: clear, rain, thunder");
		}
		return builder.addCriterion(new CriterionWeather(w));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRequiredTime(ResearchBuilder builder, int min, int max) {
		return builder.addCriterion(new CriterionTime(min, max));
	}

	@ZenCodeType.Method
	public static ResearchBuilder addXPCondition(ResearchBuilder builder, int amount) {
		return builder.addConditionInternal(new ConditionExperience(amount));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRewardXP(ResearchBuilder builder, int points) {
		return builder.addReward(new RewardExperience(points, false));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setRewardXPLevels(ResearchBuilder builder, int levels) {
		return builder.addReward(new RewardExperience(levels, true));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setTriggerXP(ResearchBuilder builder, int points) {
		return builder.addTrigger(new RewardExperience(points, false));
	}

	@ZenCodeType.Method
	public static ResearchBuilder setTriggerXPLevels(ResearchBuilder builder, int levels) {
		return builder.addTrigger(new RewardExperience(levels, true));
	}

	private static Set<ResourceLocation> parseIds(String[] ids) {
		Set<ResourceLocation> set = new LinkedHashSet<>(ids.length);
		for (String id : ids) {
			ResourceLocation parsed = ResourceLocation.tryParse(id);
			if (parsed == null) {
				throw new IllegalArgumentException("Invalid ResourceLocation: " + id);
			}
			set.add(parsed);
		}
		return set;
	}
}
