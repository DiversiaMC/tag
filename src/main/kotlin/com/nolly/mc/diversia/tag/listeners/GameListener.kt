package com.nolly.mc.diversia.tag.listeners

import com.nolly.mc.diversia.tag.game.GameManager
import com.nolly.mc.diversia.tag.model.GameState
import com.nolly.mc.diversia.tag.model.Role
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class GameListener(private val game: GameManager) : Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onDamage(event: EntityDamageEvent) {
		val player = event.entity as? Player ?: return
		if (game.isInGame(player.uniqueId)) event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onHit(event: EntityDamageByEntityEvent) {
		val attacker = event.damager as? Player ?: return
		val victim = event.entity as? Player ?: return
		if (!game.isInGame(attacker.uniqueId) || !game.isInGame(victim.uniqueId)) {
			event.isCancelled = true
			return
		}
		if (game.state != GameState.RUNNING) {
			event.isCancelled = true
			return
		}
		event.isCancelled = true
		if (game.getRole(attacker.uniqueId) == Role.CAT && game.getRole(victim.uniqueId) == Role.MOUSE) {
			game.handleTag(attacker, victim)
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onMove(event: PlayerMoveEvent) {
		val player = event.player
		if (player.uniqueId !in game.lockedInBox) return
		val from = event.from
		val to = event.to ?: return
		if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
			event.isCancelled = true
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onDrop(event: PlayerDropItemEvent) {
		if (game.isInGame(event.player.uniqueId)) event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onInvClick(event: InventoryClickEvent) {
		val player = event.whoClicked as? Player ?: return
		if (game.isInGame(player.uniqueId)) event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onInvDrag(event: InventoryDragEvent) {
		val player = event.whoClicked as? Player ?: return
		if (game.isInGame(player.uniqueId)) event.isCancelled = true
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	fun onHunger(event: FoodLevelChangeEvent) {
		val player = event.entity as? Player ?: return
		if (!game.isInGame(player.uniqueId)) {
			event.isCancelled = true
			event.foodLevel = 20
			return
		}
		event.isCancelled = true
		event.foodLevel = 20
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	fun onQuit(event: PlayerQuitEvent) {
		val player = event.player
		if (!game.isInGame(player.uniqueId)) return
		game.leaveGame(player)
	}

	@EventHandler(priority = EventPriority.LOWEST)
	fun onJoin(event: PlayerJoinEvent) {
		val player = event.player
		player.teleport(game.config.resolveLobbyLocation())
		if (game.state == GameState.RUNNING || game.state == GameState.PAUSED) {
			if (player.gameMode != GameMode.CREATIVE) player.gameMode = GameMode.SPECTATOR
		}
	}
}
