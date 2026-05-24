package snownee.researchtable.plugin.minecraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import snownee.researchtable.Registration;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.CriterionType;
import snownee.researchtable.core.ICriterion;

public class CriterionBiome implements ICriterion {
	private final Set<ResourceLocation> biomes;

	public CriterionBiome(Set<ResourceLocation> biomes) {
		this.biomes = biomes;
	}

	@Override
	public boolean matches(Player player, CompoundTag data) {
		Holder<Biome> holder = player.level().getBiome(player.blockPosition());
		return holder.unwrapKey()
				.map(key -> biomes.contains(key.location()))
				.orElse(false);
	}

	@Override
	public String getFailingText(Player player, CompoundTag data) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (ResourceLocation id : biomes) {
			if (!first) {
				sb.append(ChatFormatting.RESET).append(", ");
			}
			first = false;
			sb.append(ChatFormatting.AQUA).append(id);
		}
		sb.append(ChatFormatting.RESET);
		return I18n.get(ResearchTable.MODID + ".gui.needBiome", sb.toString());
	}

	public static final CriterionType<CriterionBiome> TYPE = CriterionType.register(
			Registration.id("biome"),
			(buf, c) -> ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC)
					.encode(buf, new ArrayList<>(c.biomes)),
			buf -> {
				ArrayList<ResourceLocation> list = ByteBufCodecs.collection(
						ArrayList::new, ResourceLocation.STREAM_CODEC).decode(buf);
				return new CriterionBiome(new LinkedHashSet<>(list));
			});

	@Override
	public CriterionType<CriterionBiome> getType() {
		return TYPE;
	}
}
