package snownee.researchtable.plugin.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.styles.ProgressStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.researchtable.Registration;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.block.TileTable;
import snownee.researchtable.core.Research;

public class TableInfoProvider implements IProbeInfoProvider {

	@Override
	public ResourceLocation getID() {
		return ResourceLocation.fromNamespaceAndPath(ResearchTable.MODID, "tableinfo");
	}

	@Override
	public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, Player player, Level level, BlockState blockState, IProbeHitData iProbeHitData) {
		if (probeMode == ProbeMode.EXTENDED || probeMode == ProbeMode.DEBUG) {
			if (blockState.getBlock() == Registration.TABLE_BLOCK.get()) {
				BlockEntity tile = level.getBlockEntity(iProbeHitData.getPos());
				if (tile instanceof TileTable table) {
					if (!table.ownerName.isEmpty()) {
						iProbeInfo.text(I18n.get(ResearchTable.MODID + ".gui.owner", ChatFormatting.WHITE + table.ownerName));
					}
					Research research = table.getResearch();
					if (research != null) {
						String title = research.getTitleRaw();
						if (I18n.exists(title)) {
							title = I18n.get(title);
						}
						iProbeInfo.text(I18n.get(ResearchTable.MODID + ".gui.researching", ChatFormatting.WHITE + title));
						iProbeInfo.progress((int) (table.getProgress()), 100, new ProgressStyle().filledColor(0xFF00CC33).alternateFilledColor(0xFF00CC33).backgroundColor(0).suffix("%"));
					}
				}
			}
		}
	}
}
