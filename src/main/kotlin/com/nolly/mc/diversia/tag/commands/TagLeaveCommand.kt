package com.nolly.mc.diversia.tag.commands

import com.nolly.mc.diversia.tag.config.TagConfig
import com.nolly.mc.diversia.tag.game.GameManager
import com.nolly.mc.diversia.tag.model.GameState
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TagLeaveCommand(
	private val config: TagConfig,
	private val game: GameManager
) : CommandExecutor {

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		if (sender !is Player) {
			sender.sendMessage(TextAPI.parse(config.messages.playersOnly))
			return true
		}
		if (game.state == GameState.RUNNING || game.state == GameState.PAUSED) {
			TextAPI.send(sender, config.messages.gameNotJoinable)
			return true
		}
		if (!game.isInGame(sender.uniqueId)) {
			TextAPI.send(sender, config.messages.notInGame)
			return true
		}
		game.leaveGame(sender)
		TextAPI.send(sender, config.messages.leftGame)
		return true
	}
}
