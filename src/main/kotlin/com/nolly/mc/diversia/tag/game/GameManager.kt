package com.nolly.mc.diversia.tag.game

import com.nolly.mc.diversia.tag.config.TagConfig
import com.nolly.mc.diversia.tag.model.GameState
import com.nolly.mc.diversia.tag.model.Role
import com.nolly.mc.diversia.tag.model.TagPlayer
import com.nolly.mc.textapi.api.TextAPI
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class GameManager(
	private val plugin: JavaPlugin, val config: TagConfig
) {
	var state: GameState = GameState.IDLE
		private set

	val spawnBoxManager = SpawnBoxManager(config)
	val lockedInBox = mutableSetOf<UUID>()

	private val players = mutableMapOf<UUID, TagPlayer>()
	private var timerTaskId: Int = -1
	private var countdownTaskId: Int = -1
	var timeRemainingSeconds: Int = 0
		private set

	fun enterSetup() {
		state = GameState.SETUP
	}

	fun enterWaiting() {
		state = GameState.WAITING
	}

	fun forceRole(uuid: UUID, role: Role) {
		players[uuid]?.role = role
	}

	fun startGame() {
		state = GameState.RUNNING
		timeRemainingSeconds = config.timeLimitSeconds

		val catSpawns = config.loadSpawns("cat")
		val mouseSpawns = config.loadSpawns("mouse")

		catSpawns.forEachIndexed { i, data -> spawnBoxManager.placeBox("cat_$i", data) }
		mouseSpawns.forEachIndexed { i, data -> spawnBoxManager.placeBox("mouse_$i", data) }

		teleportAllToSpawns(catSpawns, mouseSpawns)
		lockedInBox.addAll(players.keys)
		setNonGamePlayersToSpectator()
		startCountdown(10)
	}

	private fun startCountdown(seconds: Int) {
		var remaining = seconds
		countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (remaining > 0) {
				players.keys.forEach { uuid ->
					Bukkit.getPlayer(uuid)?.sendTitle("§e$remaining", "§7Get ready!", 5, 25, 5)
				}
				remaining--
			} else {
				spawnBoxManager.restoreAll()
				lockedInBox.clear()
				startTimerTask()
				broadcast(config.messages.gameStarted)
				Bukkit.getScheduler().cancelTask(countdownTaskId)
				countdownTaskId = -1
			}
		}, 0L, 20L).taskId
	}

	fun pauseGame() {
		if (state != GameState.RUNNING) return
		state = GameState.PAUSED
		stopTimerTask()
		broadcast(config.messages.gamePaused)
	}

	fun resumeGame() {
		if (state != GameState.PAUSED) return
		state = GameState.RUNNING
		startTimerTask()
		broadcast(config.messages.gameResumed)
	}

	fun stopGame() {
		endGame(forced = true)
		broadcast(config.messages.gameStopped)
	}

	fun resetGame() {
		stopAllTasks()
		spawnBoxManager.restoreAll()
		lockedInBox.clear()
		restoreAllInventories()
		teleportAllToLobby()
		players.clear()
		state = GameState.IDLE
		broadcast(config.messages.gameReset)
	}

	fun reloadConfig() {
		config.reload()
	}

	fun joinRole(player: Player, role: Role): Boolean {
		if (state !in listOf(GameState.SETUP, GameState.WAITING)) return false
		saveInventoryAndClear(player)
		players[player.uniqueId] = TagPlayer(uuid = player.uniqueId, role = role)
		player.gameMode = GameMode.ADVENTURE
		return true
	}

	fun leaveGame(player: Player) {
		players.remove(player.uniqueId)
		restoreInventory(player)
		teleportToLobby(player)
		player.gameMode = GameMode.ADVENTURE
	}

	fun isInGame(uuid: UUID): Boolean = players.containsKey(uuid)

	fun getRole(uuid: UUID): Role? = players[uuid]?.role

	fun getMiceCount(): Int = players.values.count { it.role == Role.MOUSE }
	fun getCatsCount(): Int = players.values.count { it.role == Role.CAT }

	fun handleTag(tagger: Player, tagged: Player) {
		if (state != GameState.RUNNING) return
		val taggerData = players[tagger.uniqueId] ?: return
		val taggedData = players[tagged.uniqueId] ?: return
		if (taggerData.role != Role.CAT) return
		if (taggedData.role != Role.MOUSE) return

		taggedData.role = Role.CAT

		val catSpawns = config.loadSpawns("cat")
		if (catSpawns.isNotEmpty()) {
			val nearest = catSpawns.minByOrNull { spawn ->
				val world = Bukkit.getWorld(spawn.world) ?: return@minByOrNull Double.MAX_VALUE
				tagged.location.distanceSquared(Location(world, spawn.x, spawn.y, spawn.z))
			}
			nearest?.let { spawn ->
				val world = Bukkit.getWorld(spawn.world) ?: return@let
				tagged.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
			}
		}

		broadcast(
			config.messages.playerTagged.replace("{tagger}", tagger.name).replace("{tagged}", tagged.name)
		)
		broadcast(
			config.messages.miceRemaining.replace("{count}", getMiceCount().toString())
		)

		checkWinCondition()
	}

	private fun checkWinCondition() {
		if (getMiceCount() == 0) {
			endGame(catsWin = true)
		}
	}

	private fun endGame(catsWin: Boolean = false, forced: Boolean = false) {
		state = GameState.ENDED
		stopAllTasks()
		spawnBoxManager.restoreAll()
		lockedInBox.clear()

		if (!forced) {
			if (catsWin) {
				broadcast(config.messages.gameWinCats)
			} else {
				broadcast(
					config.messages.gameWinMice.replace("{count}", getMiceCount().toString())
				)
			}
		}

		restoreAllInventories()
		teleportAllToLobby()
		players.clear()
	}

	private fun startTimerTask() {
		timerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
			if (state != GameState.RUNNING) return@Runnable
			if (timeRemainingSeconds <= 0) {
				endGame(catsWin = false)
				return@Runnable
			}
			timeRemainingSeconds--
		}, 20L, 20L).taskId
	}

	private fun stopTimerTask() {
		if (timerTaskId != -1) Bukkit.getScheduler().cancelTask(timerTaskId)
		timerTaskId = -1
	}

	private fun stopAllTasks() {
		stopTimerTask()
		if (countdownTaskId != -1) Bukkit.getScheduler().cancelTask(countdownTaskId)
		countdownTaskId = -1
	}

	fun broadcast(message: String) {
		Bukkit.getOnlinePlayers().forEach { TextAPI.send(it, message) }
	}

	private fun teleportAllToSpawns(
		catSpawns: List<TagConfig.LocationData>, mouseSpawns: List<TagConfig.LocationData>
	) {
		var catIndex = 0
		var mouseIndex = 0
		players.values.forEach { tp ->
			val player = Bukkit.getPlayer(tp.uuid) ?: return@forEach
			val spawns = if (tp.role == Role.CAT) catSpawns else mouseSpawns
			if (spawns.isEmpty()) return@forEach
			val spawn = if (tp.role == Role.CAT) {
				catSpawns[catIndex++ % catSpawns.size]
			} else {
				mouseSpawns[mouseIndex++ % mouseSpawns.size]
			}
			val world = Bukkit.getWorld(spawn.world) ?: return@forEach
			player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
		}
	}

	private fun setNonGamePlayersToSpectator() {
		Bukkit.getOnlinePlayers().forEach { player ->
			if (isInGame(player.uniqueId)) return@forEach
			if (player.gameMode == GameMode.CREATIVE) return@forEach
			player.gameMode = GameMode.SPECTATOR
		}
	}

	private fun teleportToLobby(player: Player) {
		player.teleport(config.resolveLobbyLocation())
	}

	private fun teleportAllToLobby() {
		val loc = config.resolveLobbyLocation()
		players.keys.toList().forEach { uuid ->
			Bukkit.getPlayer(uuid)?.let { player ->
				player.gameMode = GameMode.SURVIVAL
				player.teleport(loc)
			}
		}
		Bukkit.getOnlinePlayers().filter { it.uniqueId !in players && it.gameMode == GameMode.SPECTATOR }
			.forEach { player ->
				player.gameMode = GameMode.SURVIVAL
				player.teleport(loc)
			}
	}

	private fun saveInventoryAndClear(player: Player) {
		val tp = players.getOrPut(player.uniqueId) {
			TagPlayer(uuid = player.uniqueId, role = Role.MOUSE)
		}
		tp.savedInventory = player.inventory.contents.copyOf()
		player.inventory.clear()
	}

	@Suppress("UnstableApiUsage")
	private fun restoreInventory(player: Player) {
		val tp = players[player.uniqueId] ?: return
		player.inventory.contents = tp.savedInventory
		player.updateInventory()
	}

	@Suppress("UnstableApiUsage")
	private fun restoreAllInventories() {
		players.values.forEach { tp ->
			val player = Bukkit.getPlayer(tp.uuid) ?: return@forEach
			player.inventory.contents = tp.savedInventory
			player.updateInventory()
		}
	}
}
