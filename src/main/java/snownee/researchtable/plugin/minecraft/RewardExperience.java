package snownee.researchtable.plugin.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import snownee.researchtable.api.IReward;

public class RewardExperience implements IReward {
	private final int amount;
	private final boolean asLevels;

	public RewardExperience(int amount, boolean asLevels) {
		this.amount = Math.max(0, amount);
		this.asLevels = asLevels;
	}

	@Override
	public void earn(Level world, BlockPos pos, Player player) {
		if (asLevels) {
			player.giveExperienceLevels(amount);
		} else {
			player.giveExperiencePoints(amount);
		}
	}
}
