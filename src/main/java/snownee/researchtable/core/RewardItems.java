package snownee.researchtable.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class RewardItems implements IReward {
	private final NonNullList<ItemStack> items;

	public RewardItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public void earn(Level world, BlockPos pos, Player player) {
		for (ItemStack item : items)
			ItemHandlerHelper.giveItemToPlayer(player, item);
	}

}
