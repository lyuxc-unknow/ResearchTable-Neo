package snownee.researchtable.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.core.CriterionType;

public interface ICriterion {
	boolean matches(Player player, CompoundTag data);

	String getFailingText(Player player, CompoundTag data);

	CriterionType<? extends ICriterion> getType();
}
