package snownee.researchtable.block;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.EventOpenTable;
import snownee.researchtable.core.Research;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockTable extends HorizontalDirectionalBlock implements EntityBlock {

	public static final com.mojang.serialization.MapCodec<BlockTable> CODEC = simpleCodec($ -> new BlockTable());

	private static final VoxelShape SHAPE = Shapes.create(new AABB(0.1, 0, 0.1, 0.9, 0.9, 0.9));

	public BlockTable() {
		super(Properties.of()
				.mapColor(MapColor.METAL)
				.strength(2.5F)
				.sound(SoundType.METAL)
				.lightLevel(s -> 8)
				.pushReaction(PushReaction.BLOCK)
				.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileTable(pos, state);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		ItemStack copy = stack.copy();
		IFluidHandlerItem fluidHandler = copy.getCapability(Capabilities.FluidHandler.ITEM);
		if (fluidHandler != null) {
			IFluidHandler fluidDestination = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, hitResult.getDirection());
			if (fluidDestination != null) {
				FluidStack drained = fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
				if (!drained.isEmpty()) {
					int filled = fluidDestination.fill(drained, IFluidHandler.FluidAction.SIMULATE);
					if (filled > 0) {
						FluidStack reallyDrained = fluidHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
						fluidDestination.fill(reallyDrained, IFluidHandler.FluidAction.EXECUTE);
						if (!player.isCreative()) {
							if (stack.getCount() > 1) {
								stack.shrink(1);
								ItemHandlerHelper.giveItemToPlayer(player, fluidHandler.getContainer());
							} else {
								player.setItemInHand(hand, fluidHandler.getContainer());
							}
						}
						return ItemInteractionResult.sidedSuccess(level.isClientSide);
					}
				}
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof TileTable table) {
			if (!table.hasPermission(player)) {
				player.sendSystemMessage(Component.translatable(ResearchTable.MODID + ".noPermission"));
				return InteractionResult.CONSUME;
			}
			if (!level.isClientSide) {
				if (NeoForge.EVENT_BUS.post(new EventOpenTable(player, table)).isCanceled()) {
					return InteractionResult.CONSUME;
				}
				table.putOwnerInfo(player);
				if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
					sp.openMenu(table, buf -> buf.writeBlockPos(pos));
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag compound = customData != null ? customData.copyTag() : null;
		if (compound != null) {
			if (compound.contains("BlockEntityTag", 10)) {
				String ownerName = null;
				CompoundTag tileCompound = compound.getCompound("BlockEntityTag");
				if (tileCompound.contains("owner", 10)) {
					ownerName = tileCompound.getCompound("owner").getString("name");
				} else if (tileCompound.contains("owner", 8)) {
					ownerName = tileCompound.getString("owner");
				}
				if (ownerName != null && !ownerName.isEmpty()) {
					tooltip.add(Component.translatable(ResearchTable.MODID + ".gui.owner",
							Component.literal(ownerName).withStyle(ChatFormatting.RESET)).withStyle(ChatFormatting.GRAY));
				}
			}
			if (compound.contains("title", 8)) {
				String title = compound.getString("title");
				if (I18n.exists(title)) {
					title = I18n.get(title);
				}
				tooltip.add(Component.translatable(ResearchTable.MODID + ".gui.researching",
						Component.literal(title).withStyle(ChatFormatting.RESET)).withStyle(ChatFormatting.GRAY));
				if (compound.contains("progress", 5)) {
					float progress = compound.getFloat("progress");
					tooltip.add(Component.translatable(ResearchTable.MODID + ".gui.progress",
							Component.literal(String.format("%.2f%%", progress)).withStyle(ChatFormatting.RESET))
							.withStyle(ChatFormatting.GRAY));
				}
			}
		}
		super.appendHoverText(stack, context, tooltip, flagIn);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide) {
			BlockEntity tile = level.getBlockEntity(pos);
			if (tile instanceof TileTable table) {
				ItemStack stack = new ItemStack(this);
				CompoundTag tileCompound = table.saveWithoutMetadata(level.registryAccess());
				CompoundTag compound = new CompoundTag();
				compound.put("BlockEntityTag", tileCompound);
				Research research = table.getResearch();
				if (research != null) {
					compound.putString("title", research.getTitleRaw());
					compound.putFloat("progress", table.getProgress());
				}
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
				ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
				entity.setDefaultPickUpDelay();
				level.addFreshEntity(entity);
			}
		}
		super.playerWillDestroy(level, pos, state, player);
		return state;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && placer instanceof Player player) {
			BlockEntity tile = level.getBlockEntity(pos);
			if (tile instanceof TileTable t) {
				t.putOwnerInfo(player);
			}
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof TileTable table) {
			if (table.getResearch() == null) {
				return 0;
			}
			if (table.canComplete()) {
				return 15;
			}
			return 1 + Mth.ceil(table.getProgress() * 0.13f);
		}
		return 0;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos fromPos, boolean isMoving) {
		BlockEntity tile = level.getBlockEntity(pos);
		if (tile instanceof TileTable table) {
			table.powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
		}
	}
}
