package snownee.researchtable.plugin.astage;

import java.util.Set;

import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesUtils;
import com.google.common.collect.ImmutableSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import snownee.researchtable.core.IReward;

public class RewardUnlockStages implements IReward {
	private final Set<String> stages;

	public RewardUnlockStages(String... stages) {
		this.stages = ImmutableSet.copyOf(stages);
	}

	@Override
	public void earn(Level world, BlockPos pos, Player player) {
		stages.forEach(e -> AStagesUtils.addStage(AHolder.player(player.getUUID()), e, true));
	}
}
