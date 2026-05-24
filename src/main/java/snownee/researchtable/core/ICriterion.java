package snownee.researchtable.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface ICriterion {
	boolean matches(Player player, CompoundTag data);

	String getFailingText(Player player, CompoundTag data);

	CriterionType<? extends ICriterion> getType();
}
