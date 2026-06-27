package com.nolly.mc.diversia.tag.config

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class TagConfig(private val plugin: JavaPlugin) {
	private lateinit var cfg: FileConfiguration

	fun reload() {
		runCatching { plugin.saveDefaultConfig() }
		plugin.reloadConfig()
		cfg = plugin.config
	}

	init {
		reload()
	}

	val prefix: String get() = cfg.getString("prefix").orEmpty()

	val timeLimitSeconds: Int get() = cfg.getInt("game.time-limit-seconds", 180)
	val minPlayers: Int get() = cfg.getInt("game.min-players", 2)
	val openJoin: Boolean get() = cfg.getBoolean("game.open-join", true)

	fun setOpenJoin(value: Boolean) {
		cfg.set("game.open-join", value)
		plugin.saveConfig()
	}

	fun resolveLobbyLocation(): Location {
		val world =
			cfg.getString("lobby.world")?.let { Bukkit.getWorld(it) } ?: return Bukkit.getWorlds().first().spawnLocation
		return Location(
			world,
			cfg.getDouble("lobby.x"),
			cfg.getDouble("lobby.y"),
			cfg.getDouble("lobby.z"),
			cfg.getDouble("lobby.yaw").toFloat(),
			cfg.getDouble("lobby.pitch").toFloat()
		)
	}

	fun saveLobby(location: Location) {
		cfg.set("lobby.world", location.world?.name)
		cfg.set("lobby.x", location.x)
		cfg.set("lobby.y", location.y)
		cfg.set("lobby.z", location.z)
		cfg.set("lobby.yaw", location.yaw.toDouble())
		cfg.set("lobby.pitch", location.pitch.toDouble())
		plugin.saveConfig()
	}

	fun loadSpawns(role: String): List<LocationData> {
		val list = cfg.getMapList("spawns.${role.lowercase()}")
		return list.mapNotNull { map ->
			val world = map["world"] as? String ?: return@mapNotNull null
			LocationData(
				world = world,
				x = (map["x"] as? Number)?.toDouble() ?: 0.0,
				y = (map["y"] as? Number)?.toDouble() ?: 64.0,
				z = (map["z"] as? Number)?.toDouble() ?: 0.0,
				yaw = (map["yaw"] as? Number)?.toFloat() ?: 0f,
				pitch = (map["pitch"] as? Number)?.toFloat() ?: 0f
			)
		}
	}

	fun addSpawn(role: String, location: Location) {
		val key = "spawns.${role.lowercase()}"
		val existing = cfg.getMapList(key).toMutableList()
		existing.add(
			mapOf(
				"world" to (location.world?.name ?: "world"),
				"x" to location.x,
				"y" to location.y,
				"z" to location.z,
				"yaw" to location.yaw.toDouble(),
				"pitch" to location.pitch.toDouble()
			)
		)
		cfg.set(key, existing)
		plugin.saveConfig()
	}

	fun removeSpawn(role: String, index: Int): Boolean {
		val key = "spawns.${role.lowercase()}"
		val existing = cfg.getMapList(key).toMutableList()
		if (index < 0 || index >= existing.size) return false
		existing.removeAt(index)
		cfg.set(key, existing)
		plugin.saveConfig()
		return true
	}

	private fun resolve(message: String): String = message.replace("{prefix}", prefix)

	val messages: MessagesConfig
		get() = MessagesConfig(
			noPermission = resolve(cfg.getString("messages.no-permission").orEmpty()),
			playersOnly = resolve(cfg.getString("messages.players-only").orEmpty()),
			invalidUsage = resolve(cfg.getString("messages.invalid-usage").orEmpty()),
			reload = resolve(cfg.getString("messages.reload").orEmpty()),
			gameAlreadyRunning = resolve(cfg.getString("messages.game-already-running").orEmpty()),
			gameNotRunning = resolve(cfg.getString("messages.game-not-running").orEmpty()),
			gameNotSetup = resolve(cfg.getString("messages.game-not-setup").orEmpty()),
			gameNotWaiting = resolve(cfg.getString("messages.game-not-waiting").orEmpty()),
			gameStarted = resolve(cfg.getString("messages.game-started").orEmpty()),
			gameStopped = resolve(cfg.getString("messages.game-stopped").orEmpty()),
			gameReset = resolve(cfg.getString("messages.game-reset").orEmpty()),
			gamePaused = resolve(cfg.getString("messages.game-paused").orEmpty()),
			gameResumed = resolve(cfg.getString("messages.game-resumed").orEmpty()),
			gameWinCats = resolve(cfg.getString("messages.game-win-cats").orEmpty()),
			gameWinMice = resolve(cfg.getString("messages.game-win-mice").orEmpty()),
			gameTie = resolve(cfg.getString("messages.game-tie").orEmpty()),
			spawnAdded = resolve(cfg.getString("messages.spawn-added").orEmpty()),
			spawnRemoved = resolve(cfg.getString("messages.spawn-removed").orEmpty()),
			spawnNotFound = resolve(cfg.getString("messages.spawn-not-found").orEmpty()),
			lobbySet = resolve(cfg.getString("messages.lobby-set").orEmpty()),
			joinedRole = resolve(cfg.getString("messages.joined-role").orEmpty()),
			leftGame = resolve(cfg.getString("messages.left-game").orEmpty()),
			notInGame = resolve(cfg.getString("messages.not-in-game").orEmpty()),
			roleFull = resolve(cfg.getString("messages.role-full").orEmpty()),
			gameNotJoinable = resolve(cfg.getString("messages.game-not-joinable").orEmpty()),
			playerTagged = resolve(cfg.getString("messages.player-tagged").orEmpty()),
			miceRemaining = resolve(cfg.getString("messages.mice-remaining").orEmpty()),
			statusHeader = resolve(cfg.getString("messages.status-header").orEmpty()),
			statusState = resolve(cfg.getString("messages.status-state").orEmpty()),
			statusTime = resolve(cfg.getString("messages.status-time").orEmpty()),
			statusMice = resolve(cfg.getString("messages.status-mice").orEmpty()),
			statusCats = resolve(cfg.getString("messages.status-cats").orEmpty())
		)

	data class LocationData(
		val world: String, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float
	)

	data class MessagesConfig(
		val noPermission: String,
		val playersOnly: String,
		val invalidUsage: String,
		val reload: String,
		val gameAlreadyRunning: String,
		val gameNotRunning: String,
		val gameNotSetup: String,
		val gameNotWaiting: String,
		val gameStarted: String,
		val gameStopped: String,
		val gameReset: String,
		val gamePaused: String,
		val gameResumed: String,
		val gameWinCats: String,
		val gameWinMice: String,
		val gameTie: String,
		val spawnAdded: String,
		val spawnRemoved: String,
		val spawnNotFound: String,
		val lobbySet: String,
		val joinedRole: String,
		val leftGame: String,
		val notInGame: String,
		val roleFull: String,
		val gameNotJoinable: String,
		val playerTagged: String,
		val miceRemaining: String,
		val statusHeader: String,
		val statusState: String,
		val statusTime: String,
		val statusMice: String,
		val statusCats: String
	)
}
