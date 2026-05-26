package snownee.researchtable.command;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import snownee.researchtable.ResearchTable;
import snownee.researchtable.core.DataStorage;
import snownee.researchtable.core.Research;
import snownee.researchtable.core.ResearchList;

public class CommandResearch {

	private static final SimpleCommandExceptionType ERROR_RESEARCH_NOT_FOUND = new SimpleCommandExceptionType(Component.translatable("commands." + ResearchTable.MODID + ".researchNotFound"));

	private static final SuggestionProvider<CommandSourceStack> RESEARCH_SUGGESTIONS = (ctx, builder) -> {
		Collection<String> names = ResearchList.LIST.keySet();
		return SharedSuggestionProvider.suggest(Stream.concat(names.stream(), Stream.of("all")), builder);
	};

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal(ResearchTable.MODID)
						.requires(s -> s.hasPermission(2))
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("research", StringArgumentType.word())
										.suggests(RESEARCH_SUGGESTIONS)
										.executes(CommandResearch::executeGet)
										.then(Commands.argument("count", IntegerArgumentType.integer(0))
												.executes(CommandResearch::executeSet))))
		);
	}

	private static int executeGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
		String name = StringArgumentType.getString(ctx, "research");
		Collection<Research> researches = lookup(name);
		for (Research r : researches) {
			int count = DataStorage.count(player.getGameProfile().getId(), r);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands." + ResearchTable.MODID + ".get", player.getName().getString(), count), true);
		}
		return researches.size();
	}

	private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
		String name = StringArgumentType.getString(ctx, "research");
		int count = IntegerArgumentType.getInteger(ctx, "count");
		Collection<Research> researches = lookup(name);
		for (Research r : researches) {
			DataStorage.setCount(player.getGameProfile().getId(), r, count);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands." + ResearchTable.MODID + ".set", player.getName().getString()), true);
		}
		return researches.size();
	}

	private static Collection<Research> lookup(String name) throws CommandSyntaxException {
		if (name.equals("all")) {
			return ResearchList.LIST.values();
		}
		Optional<Research> result = ResearchList.find(name);
		if (result.isEmpty()) {
			throw ERROR_RESEARCH_NOT_FOUND.create();
		}
		return Collections.singletonList(result.get());
	}
}
