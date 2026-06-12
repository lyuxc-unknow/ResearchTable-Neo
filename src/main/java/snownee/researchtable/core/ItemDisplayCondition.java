package snownee.researchtable.core;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

public interface ItemDisplayCondition {
	List<ItemStack> getDisplayItems();

	@Nullable
	String getCustomName();
}
