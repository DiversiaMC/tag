package com.nolly.mc.diversia.tag.model

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class TagPlayer(
	val uuid: UUID,
	var role: Role,
	var savedInventory: Array<ItemStack?> = arrayOfNulls(41)
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as TagPlayer
		if (uuid != other.uuid) return false
		if (role != other.role) return false
		if (!savedInventory.contentEquals(other.savedInventory)) return false
		return true
	}

	override fun hashCode(): Int {
		var result = uuid.hashCode()
		result = 31 * result + role.hashCode()
		result = 31 * result + savedInventory.contentHashCode()
		return result
	}
}
