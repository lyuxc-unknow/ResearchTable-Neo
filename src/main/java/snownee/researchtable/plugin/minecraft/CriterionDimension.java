package snownee.researchtable.plugin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.api.ICriterion;

public class CriterionDimension implements ICriterion {
	private final Set<ResourceLocation> dimensions;

	public CriterionDimension(Set<ResourceLocation> dimensions) {
		this.dimensions = Collections.unmodifiableSet(new LinkedHashSet<>(dimensions));
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		return dimensions.contains(player.level().dimension().location());
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ResourceLocation id : dimensions) {
			if (!first) {
				sb.append(ChatFormatting.RESET).append(", ");
			}
			first = false;
			sb.append(ChatFormatting.LIGHT_PURPLE).append(id);
		}
		sb.append(ChatFormatting.RESET);
		return I18n.get(ResearchTable.MODID + ".gui.needDimension", sb.toString());
	}

	public static final CriterionType<CriterionDimension> TYPE = CriterionType.register(
			ResearchTable.id("dimension"),
			(buf, c) -> ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC)
					.encode(buf, new ArrayList<>(c.dimensions)),
			buf -> {
				ArrayList<ResourceLocation> list = ByteBufCodecs.collection(
						ArrayList::new, ResourceLocation.STREAM_CODEC).decode(buf);
				return new CriterionDimension(new LinkedHashSet<>(list));
			});

	@Override
	public CriterionType<CriterionDimension> getType() {
		return TYPE;
	}
}
