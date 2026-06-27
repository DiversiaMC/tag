package com.nolly.mc.diversia.tag

import com.nolly.mc.diversia.tag.commands.TagCommand
import com.nolly.mc.diversia.tag.commands.TagJoinCommand
import com.nolly.mc.diversia.tag.commands.TagLeaveCommand
import com.nolly.mc.diversia.tag.config.TagConfig
import com.nolly.mc.diversia.tag.game.GameManager
import com.nolly.mc.diversia.tag.listeners.GameListener
import com.nolly.mc.diversia.tag.listeners.NoCommandListener
import org.bukkit.plugin.java.JavaPlugin

class DiversiaTag : JavaPlugin() {
	private lateinit var tagConfig: TagConfig
	private lateinit var gameManager: GameManager

	override fun onEnable() {
		logger.info("[Diversia] Tag plugin enabling...")
		try {
			tagConfig = TagConfig(this)
			gameManager = GameManager(this, tagConfig)

			val tagCommand = TagCommand(tagConfig, gameManager)
			getCommand("tag")?.apply {
				setExecutor(tagCommand)
				tabCompleter = tagCommand
			}

			val joinCommand = TagJoinCommand(tagConfig, gameManager)
			getCommand("tagjoin")?.apply {
				setExecutor(joinCommand)
				tabCompleter = joinCommand
			}

			getCommand("tagleave")?.setExecutor(TagLeaveCommand(tagConfig, gameManager))

			server.pluginManager.registerEvents(GameListener(gameManager), this)
			server.pluginManager.registerEvents(NoCommandListener(tagConfig), this)

			logger.info("[Diversia] Tag plugin enabled.")
		} catch (e: Exception) {
			logger.severe("[Diversia] Failed to enable Tag plugin: ${e.message}")
			e.printStackTrace()
			server.pluginManager.disablePlugin(this)
		}
	}

	override fun onDisable() {
		logger.info("[Diversia] Tag plugin disabling...")
		if (::gameManager.isInitialized) gameManager.resetGame()
		logger.info("[Diversia] Tag plugin disabled.")
	}
}
