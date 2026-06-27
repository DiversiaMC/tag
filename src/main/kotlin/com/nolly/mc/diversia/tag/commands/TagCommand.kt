package com.nolly.mc.diversia.tag.commands

import com.nolly.mc.diversia.tag.config.TagConfig
import com.nolly.mc.diversia.tag.game.GameManager
import com.nolly.mc.diversia.tag.model.GameState
import com.nolly.mc.diversia.tag.model.Role
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TagCommand(
	private val config: TagConfig, private val game: GameManager
) : CommandExecutor, TabCompleter {

	companion object {
		const val PERMISSION = "diversia.tag.master"
	}

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		if (!sender.isOp && !sender.hasPermission(PERMISSION)) {
			if (sender is Player) TextAPI.send(sender, config.messages.noPermission)
			return true
		}
		if (args.isEmpty()) {
			sendUsage(sender, label); return true
		}

		return when (args[0].lowercase()) {
			"setup" -> handleSetup(sender)
			"wait", "waiting" -> handleWaiting(sender)
			"start" -> handleStart(sender)
			"pause" -> handlePause(sender)
			"resume" -> handleResume(sender)
			"stop" -> handleStop(sender)
			"reset" -> handleReset(sender)
			"reload" -> handleReload(sender)
			"status" -> handleStatus(sender)
			"lobby" -> handleLobby(sender)
			"spawn" -> handleSpawn(sender, args.drop(1), label)
			"openjoin" -> handleOpenJoin(sender, args.drop(1))
			"forcerole" -> handleForceRole(sender, args.drop(1), label)
			else -> {
				sendUsage(sender, label); true
			}
		}
	}

	private fun handleSetup(sender: CommandSender): Boolean {
		if (game.state != GameState.IDLE && game.state != GameState.ENDED) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameAlreadyRunning)
			return true
		}
		game.enterSetup()
		if (sender is Player) TextAPI.send(sender, "<gray>Setup initialized.</gray>")
		return true
	}

	private fun handleWaiting(sender: CommandSender): Boolean {
		if (game.state != GameState.SETUP) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotSetup)
			return true
		}
		game.enterWaiting()
		if (sender is Player) TextAPI.send(sender, "<gray>Now waiting for players.</gray>")
		return true
	}

	private fun handleStart(sender: CommandSender): Boolean {
		if (game.state !in listOf(GameState.WAITING, GameState.SETUP)) {
			if (sender is Player) TextAPI.send(
				sender,
				if (game.state == GameState.IDLE) config.messages.gameNotSetup else config.messages.gameAlreadyRunning
			)
			return true
		}
		if (game.state == GameState.SETUP) game.enterWaiting()
		game.startGame()
		return true
	}

	private fun handlePause(sender: CommandSender): Boolean {
		if (game.state != GameState.RUNNING) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.pauseGame()
		return true
	}

	private fun handleResume(sender: CommandSender): Boolean {
		if (game.state != GameState.PAUSED) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.resumeGame()
		return true
	}

	private fun handleStop(sender: CommandSender): Boolean {
		if (game.state !in listOf(GameState.RUNNING, GameState.PAUSED, GameState.WAITING)) {
			if (sender is Player) TextAPI.send(sender, config.messages.gameNotRunning)
			return true
		}
		game.stopGame()
		return true
	}

	private fun handleReset(sender: CommandSender): Boolean {
		game.resetGame()
		return true
	}

	private fun handleReload(sender: CommandSender): Boolean {
		game.reloadConfig()
		if (sender is Player) TextAPI.send(sender, config.messages.reload)
		return true
	}

	private fun handleStatus(sender: CommandSender): Boolean {
		if (sender !is Player) return true
		val m = config.messages
		TextAPI.send(sender, m.statusHeader)
		TextAPI.send(sender, m.statusState.replace("{state}", game.state.name))
		val min = game.timeRemainingSeconds / 60
		val sec = game.timeRemainingSeconds % 60
		TextAPI.send(sender, m.statusTime.replace("{time}", "%02d:%02d".format(min, sec)))
		TextAPI.send(sender, m.statusMice.replace("{count}", game.getMiceCount().toString()))
		TextAPI.send(sender, m.statusCats.replace("{count}", game.getCatsCount().toString()))
		return true
	}

	private fun handleLobby(sender: CommandSender): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}
		config.saveLobby(sender.location)
		TextAPI.send(sender, config.messages.lobbySet)
		return true
	}

	private fun handleSpawn(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}
		if (args.size < 2) {
			TextAPI.send(
				sender,
				config.messages.invalidUsage.replace("{usage}", "/$label spawn <cat|mouse> <add|list|remove <index>>")
			)
			return true
		}
		val role = args[0].lowercase()
		if (role != "cat" && role != "mouse") {
			TextAPI.send(
				sender,
				config.messages.invalidUsage.replace("{usage}", "/$label spawn <cat|mouse> <add|list|remove <index>>")
			)
			return true
		}
		return when (args[1].lowercase()) {
			"add" -> {
				config.addSpawn(role, sender.location)
				val index = config.loadSpawns(role).size - 1
				TextAPI.send(
					sender, config.messages.spawnAdded.replace("{role}", role).replace("{index}", index.toString())
				)
				true
			}

			"list" -> {
				val spawns = config.loadSpawns(role)
				if (spawns.isEmpty()) TextAPI.send(sender, "<gray>No $role spawns set.</gray>")
				spawns.forEachIndexed { i, s ->
					TextAPI.send(
						sender, "<gray>#$i — ${s.world} ${s.x.toInt()} ${s.y.toInt()} ${s.z.toInt()}</gray>"
					)
				}
				true
			}

			"remove" -> {
				if (args.size < 3) {
					TextAPI.send(
						sender, config.messages.invalidUsage.replace("{usage}", "/$label spawn $role remove <index>")
					)
					return true
				}
				val index = args[2].toIntOrNull()
				if (index == null || !config.removeSpawn(role, index)) {
					TextAPI.send(
						sender, config.messages.spawnNotFound.replace("{index}", args[2]).replace("{role}", role)
					)
					return true
				}
				TextAPI.send(
					sender, config.messages.spawnRemoved.replace("{role}", role).replace("{index}", index.toString())
				)
				true
			}

			else -> {
				sendUsage(sender, label); true
			}
		}
	}

	private fun handleForceRole(sender: CommandSender, args: List<String>, label: String): Boolean {
		if (args.size < 2) {
			if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label forcerole <cat|mouse> <player>"))
			return true
		}
		val role = when (args[0].lowercase()) {
			"cat" -> Role.CAT
			"mouse" -> Role.MOUSE
			else -> {
				if (sender is Player) TextAPI.send(sender, config.messages.invalidUsage.replace("{usage}", "/$label forcerole <cat|mouse> <player>"))
				return true
			}
		}
		val target = Bukkit.getPlayerExact(args[1])
		if (target == null) {
			if (sender is Player) TextAPI.send(sender, "<red>Player not found.</red>")
			return true
		}
		if (!game.isInGame(target.uniqueId)) {
			game.joinRole(target, role)
		} else {
			game.forceRole(target.uniqueId, role)
		}
		if (sender is Player) TextAPI.send(sender, "<gray>Forced <white>${target.name}</white> into role <white>${role.name.lowercase()}</white>.</gray>")
		TextAPI.send(target, "<gray>You were forced into role <white>${role.name.lowercase()}</white>.</gray>")
		return true
	}

	private fun handleOpenJoin(sender: CommandSender, args: List<String>): Boolean {
		val newValue = if (args.isEmpty()) !config.openJoin else when (args[0].lowercase()) {
			"true", "on", "yes" -> true
			"false", "off", "no" -> false
			else -> !config.openJoin
		}
		config.setOpenJoin(newValue)
		val state = if (newValue) "<green>activated</green>" else "<red>deactivated</red>"
		if (sender is Player) TextAPI.send(sender, "<gray>Open join: $state</gray>")
		return true
	}

	private fun sendUsage(sender: CommandSender, label: String) {
		val msg =
			"<gray>Usage: <white>/$label <setup|waiting|start|pause|resume|stop|reset|reload|status|lobby|spawn|openjoin></white></gray>"
		if (sender is Player) TextAPI.send(sender, msg)
		else sender.sendMessage(TextAPI.parse(msg))
	}

	private fun filterCompletions(input: String, values: List<String>) =
		values.filter { it.startsWith(input, ignoreCase = true) }

	override fun onTabComplete(
		sender: CommandSender, command: Command, alias: String, args: Array<out String>
	): List<String> {
		if (!sender.isOp && !sender.hasPermission(PERMISSION)) return emptyList()
		val top = listOf("setup", "waiting", "start", "pause", "resume", "stop", "reset", "reload", "status", "lobby", "spawn", "openjoin", "forcerole")
		if (args.size == 1) return filterCompletions(args[0], top)
		return when (args[0].lowercase()) {
			"spawn" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("cat", "mouse"))
				3 -> filterCompletions(args[2], listOf("add", "list", "remove"))
				else -> emptyList()
			}
			"forcerole" -> when (args.size) {
				2 -> filterCompletions(args[1], listOf("cat", "mouse"))
				3 -> filterCompletions(args[2], Bukkit.getOnlinePlayers().map { it.name })
				else -> emptyList()
			}
			"openjoin" -> if (args.size == 2) filterCompletions(args[1], listOf("true", "false")) else emptyList()
			else -> emptyList()
		}
	}
}
