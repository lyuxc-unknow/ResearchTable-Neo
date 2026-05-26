package snownee.researchtable.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IReward {
	void earn(Level world, BlockPos pos, Player player);
}
