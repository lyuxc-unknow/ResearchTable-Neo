package snownee.researchtable.plugin.astage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.api.ICriterion;

public class CriterionStages implements ICriterion {
	private final Collection<String> stages;
	private final int r;

	public CriterionStages(Collection<String> stages, int requirement) {
		this.stages = List.copyOf(stages);
		int target = requirement > 0 ? requirement : this.stages.size();
		this.r = Math.min(target, this.stages.size());
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		int c = 0;
		for (String stage : stages) {
			if (AStagesUtils.hasStage(AHolder.player(player.getUUID()), stage)) {
				++c;
			}
		}
		return c >= r;
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String stage : stages) {
			if (!first) {
				sb.append(ChatFormatting.RESET).append(", ");
			}
			first = false;
			if (!AStagesUtils.hasStage(AHolder.player(player.getUUID()), stage)) {
				sb.append(ChatFormatting.GOLD);
			}
			sb.append(stage);
		}
		sb.append(ChatFormatting.RESET);
		String string = sb.toString();
		if (r == stages.size()) {
			string = I18n.get(ResearchTable.MODID + ".gui.requiredStage", string);
		} else {
			string = I18n.get(ResearchTable.MODID + ".gui.optionalStage", string);
			string += I18n.get(ResearchTable.MODID + ".gui.of", r, stages.size());
		}
		return string;
	}

	public static final CriterionType<CriterionStages> TYPE = CriterionType.register(
			ResearchTable.id("stages"),
			(buf, c) -> {
				ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buf, new ArrayList<>(c.stages));
				buf.writeVarInt(c.r);
			},
			buf -> {
				ArrayList<String> list = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf);
				Set<String> set = new LinkedHashSet<>(list);
				int r = buf.readVarInt();
				return new CriterionStages(set, r);
			});

	@Override
	public CriterionType<CriterionStages> getType() {
		return TYPE;
	}
}
