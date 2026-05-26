package snownee.researchtable.plugin.minecraft;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.api.ICriterion;

/**
 * Matches when the current world day-time (modulo 24000) falls inside [min, max].
 * If min &gt; max the range wraps midnight: [min, 24000) ∪ [0, max].
 * Reference: 0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight.
 */
public class CriterionTime implements ICriterion {
	private final int min;
	private final int max;

	public CriterionTime(int min, int max) {
		this.min = Math.floorMod(min, 24000);
		this.max = Math.floorMod(max, 24000);
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		int t = (int) Math.floorMod(player.level().getDayTime(), 24000L);
		if (min <= max) {
			return t >= min && t <= max;
		}
		return t >= min || t <= max;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		return I18n.get(ResearchTable.MODID + ".gui.needTime", min, max);
	}

	public static final CriterionType<CriterionTime> TYPE = CriterionType.register(
			ResearchTable.id("time"),
			(buf, c) -> {
				buf.writeVarInt(c.min);
				buf.writeVarInt(c.max);
			},
			buf -> new CriterionTime(buf.readVarInt(), buf.readVarInt()));

	@Override
	public CriterionType<CriterionTime> getType() {
		return TYPE;
	}
}
