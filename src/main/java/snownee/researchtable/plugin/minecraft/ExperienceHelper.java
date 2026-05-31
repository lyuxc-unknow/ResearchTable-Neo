package snownee.researchtable.plugin.minecraft;

import net.minecraft.world.entity.player.Player;

/**
 * Helpers for converting between Minecraft's level/progress XP representation and a raw point total.
 * Matches the vanilla curve used by {@link Player#getXpNeededForNextLevel()}.
 */
public final class ExperienceHelper {
	private ExperienceHelper() {
	}

	public static int xpToReachNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	/**
	 * Returns the player's total spendable XP, summed from completed levels plus the in-progress fraction.
	 * Mirrors the math vanilla uses for /xp queries.
	 */
	public static int getTotalXp(Player player) {
		int total = 0;
		for (int i = 0; i < player.experienceLevel; i++) {
			total += xpToReachNextLevel(i);
		}
		total += (int) (player.experienceProgress * xpToReachNextLevel(player.experienceLevel));
		return total;
	}

	/**
	 * Removes {@code points} of XP from the player, rebuilding level/progress from the remaining total.
	 * {@link Player#giveExperiencePoints} with a negative value also works in vanilla, but rebuilding
	 * from the total is more robust against rounding drift across many submissions.
	 */
	public static void drain(Player player, int points) {
		int remaining = Math.max(0, getTotalXp(player) - points);
		player.experienceLevel = 0;
		player.experienceProgress = 0;
		player.totalExperience = 0;
		player.giveExperiencePoints(remaining);
	}
}
