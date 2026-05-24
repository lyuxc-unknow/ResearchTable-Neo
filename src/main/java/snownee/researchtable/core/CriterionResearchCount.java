package snownee.researchtable.core;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;

public class CriterionResearchCount implements ICriterion {
	private final String id;
	private final int c;

	public CriterionResearchCount(String id, int count) {
		this.id = id;
		this.c = count;
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		return DataStorage.count(player.getGameProfile().getId(), id) < c;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		return I18n.get(ResearchTable.MODID + ".gui.maxCount", c, ChatFormatting.RED + String.valueOf(c) + ChatFormatting.RESET);
	}

	public static final CriterionType<CriterionResearchCount> TYPE = CriterionType.register(
			ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "research_count"),
			(buf, x) -> {
				buf.writeUtf(x.id);
				buf.writeVarInt(x.c);
			},
			buf -> new CriterionResearchCount(buf.readUtf(), buf.readVarInt()));

	@Override
	public CriterionType<CriterionResearchCount> getType() {
		return TYPE;
	}
}
