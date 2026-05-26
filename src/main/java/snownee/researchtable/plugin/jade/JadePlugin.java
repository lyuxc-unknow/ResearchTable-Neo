package snownee.researchtable.plugin.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.researchtable.block.BlockTable;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(TableInfoProvider.INSTANCE, BlockTable.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(TableInfoProvider.INSTANCE, BlockTable.class);
	}
}
