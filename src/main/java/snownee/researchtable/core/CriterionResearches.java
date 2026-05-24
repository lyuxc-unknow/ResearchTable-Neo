package snownee.researchtable.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;

public class CriterionResearches implements ICriterion {
	private final Collection<String> researches;
	private final int r;

	public CriterionResearches(Collection<String> researches, int requirement) {
		this.r = requirement > 0 ? requirement : researches.size();
		this.researches = researches;
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		int c = 0;
		for (String research : researches) {
			if (DataStorage.count(player.getGameProfile().getId(), research) > 0) {
				++c;
			}
		}
		return c >= r;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String research : researches) {
			Optional<Research> result = ResearchList.find(research);
			if (result.isEmpty())
				continue;
			if (!first) {
				sb.append(ChatFormatting.RESET).append(", ");
			}
			first = false;
			if (DataStorage.count(player.getGameProfile().getId(), research) == 0) {
				sb.append(ChatFormatting.RED);
			}
			sb.append(result.get().getTitle());
		}
		sb.append(ChatFormatting.RESET);
		String string = sb.toString();
		if (r == researches.size()) {
			string = I18n.get(ResearchTable.MODID + ".gui.requiredResearch", string);
		} else {
			string = I18n.get(ResearchTable.MODID + ".gui.optionalResearch", string);
			string += I18n.get(ResearchTable.MODID + ".gui.of", r, researches.size());
		}
		return string;
	}

	public static final CriterionType<CriterionResearches> TYPE = CriterionType.register(
			ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "researches"),
			(buf, c) -> {
				ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buf, new ArrayList<>(c.researches));
				buf.writeVarInt(c.r);
			},
			buf -> {
				ArrayList<String> list = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf);
				Set<String> set = new LinkedHashSet<>(list);
				int r = buf.readVarInt();
				return new CriterionResearches(set, r);
			});

	@Override
	public CriterionType<CriterionResearches> getType() {
		return TYPE;
	}
}
