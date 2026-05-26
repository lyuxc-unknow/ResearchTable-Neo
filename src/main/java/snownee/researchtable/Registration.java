package snownee.researchtable;

import java.util.function.Supplier;

import com.mojang.datafixers.DSL;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import snownee.researchtable.block.BlockTable;
import snownee.researchtable.block.TileTable;
import snownee.researchtable.client.gui.container.TableContainer;

public final class Registration {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ResearchTable.MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ResearchTable.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ResearchTable.MODID);
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ResearchTable.MODID);

	public static final Supplier<BlockTable> TABLE_BLOCK = BLOCKS.register("table", BlockTable::new);
	public static final Supplier<BlockItem> TABLE_ITEM = ITEMS.register("table", () -> new BlockItem(TABLE_BLOCK.get(), new Item.Properties()));

	public static final Supplier<BlockEntityType<TileTable>> TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register(
			"table",
			() -> BlockEntityType.Builder.of(TileTable::new, TABLE_BLOCK.get()).build(DSL.emptyPartType()));

	public static final Supplier<MenuType<TableContainer>> TABLE_MENU = MENUS.register(
			"table",
			() -> IMenuTypeExtension.create((id, inv, buf) -> {
				net.minecraft.core.BlockPos pos = buf.readBlockPos();
				return new TableContainer(id, inv, pos);
			}));

	private Registration() {
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		MENUS.register(modBus);
		modBus.addListener(Registration::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				TABLE_BLOCK_ENTITY.get(),
				(be, side) -> be.getItemHandler());
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				TABLE_BLOCK_ENTITY.get(),
				(be, side) -> be.getEnergyHandler());
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				TABLE_BLOCK_ENTITY.get(),
				(be, side) -> be.getFluidHandler());
	}
}

