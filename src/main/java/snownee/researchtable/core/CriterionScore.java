package snownee.researchtable.core;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;

public class CriterionScore implements ICriterion {
	private final String s;
	private final int min;
	private final int max;
	private final String failingText;

	public CriterionScore(String score, int min, int max, String failingText) {
		this.min = min;
		this.max = max;
		this.s = score;
		this.failingText = failingText;
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		int i = data.contains("score." + s) ? data.getInt("score." + s) : 0;
		return i >= min && i <= max;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		return I18n.get(failingText);
	}

	public static final CriterionType<CriterionScore> TYPE = CriterionType.register(
			ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "score"),
			(buf, x) -> {
				buf.writeUtf(x.s);
				buf.writeVarInt(x.min);
				buf.writeVarInt(x.max);
				buf.writeUtf(x.failingText);
			},
			buf -> new CriterionScore(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

	@Override
	public CriterionType<CriterionScore> getType() {
		return TYPE;
	}
}
