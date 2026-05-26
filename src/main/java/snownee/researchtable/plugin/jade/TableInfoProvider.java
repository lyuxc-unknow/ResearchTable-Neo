package snownee.researchtable.plugin.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.block.TileTable;
import snownee.researchtable.core.Research;

public class TableInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
	public static final TableInfoProvider INSTANCE = new TableInfoProvider();
	private static final String DATA_KEY = "ResearchTableInfo";
	private static final ResourceLocation UID = ResearchTable.id("table_info");

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		CompoundTag serverData = accessor.getServerData();
		if (!serverData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
			return;
		}
		CompoundTag tag = serverData.getCompound(DATA_KEY);
		if (tag.contains("research", Tag.TAG_STRING)) {
			String title = tag.getString("research");
			if (I18n.exists(title)) {
				title = I18n.get(title);
			}
			tooltip.add(Component.translatable(ResearchTable.MODID + ".gui.researching",
					Component.literal(title).withStyle(ChatFormatting.WHITE)));
		}
		if (tag.contains("progress", Tag.TAG_FLOAT)) {
			float progress = tag.getFloat("progress");
			tooltip.add(Component.translatable(ResearchTable.MODID + ".gui.progress",
					Component.literal(String.format("%.2f%%", progress)).withStyle(ChatFormatting.WHITE)));
		}
	}

	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (!(accessor.getBlockEntity() instanceof TileTable table)) {
			return;
		}
		CompoundTag tag = new CompoundTag();
		Research research = table.getResearch();
		if (research != null) {
			tag.putString("research", research.getTitleRaw());
			tag.putFloat("progress", table.getProgress());
		}
		if (!tag.isEmpty()) {
			data.put(DATA_KEY, tag);
		}
	}
}
