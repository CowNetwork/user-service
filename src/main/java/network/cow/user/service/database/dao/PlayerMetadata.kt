package network.cow.user.service.database.dao

import network.cow.user.service.database.table.PlayerMetadata
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID
import network.cow.user.service.database.dao.PlayerMetadata as This

/**
 * @author Benedikt Wüller
 */
class PlayerMetadata(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<This>(PlayerMetadata)
    var player by Player referencedOn PlayerMetadata.player
    var username by PlayerMetadata.username
    var locale by PlayerMetadata.locale
}
