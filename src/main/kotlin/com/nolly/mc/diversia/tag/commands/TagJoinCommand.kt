package com.nolly.mc.diversia.tag.commands

import com.nolly.mc.diversia.tag.config.TagConfig
import com.nolly.mc.diversia.tag.game.GameManager
import com.nolly.mc.diversia.tag.model.GameState
import com.nolly.mc.diversia.tag.model.Role
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TagJoinCommand(
	private val config: TagConfig,
	private val game: GameManager
) : CommandExecutor, TabCompleter {
	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}
		if (!config.openJoin || game.state !in listOf(GameState.SETUP, GameState.WAITING)) {
			TextAPI.send(sender, config.messages.gameNotJoinable)
			return true
		}
		if (args.isEmpty()) {
			TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label <cat|mouse>"))
			return true
		}
		val role = when (args[0].lowercase()) {
			"cat" -> Role.CAT
			"mouse" -> Role.MOUSE
			else -> {
				TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label <cat|mouse>"))
				return true
			}
		}
		if (!game.joinRole(sender, role)) {
			TextAPI.send(sender, config.messages.gameNotJoinable)
			return true
		}
		sender.teleport(config.resolveLobbyLocation())
		TextAPI.send(sender, config.messages.joinedRole.replace("{role}", role.name.lowercase()))
		return true
	}

	override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
		if (args.size != 1) return emptyList()
		return listOf("cat", "mouse").filter { it.startsWith(args[0], ignoreCase = true) }
	}
}
