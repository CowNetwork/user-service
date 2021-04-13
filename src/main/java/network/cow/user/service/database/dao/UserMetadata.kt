package network.cow.user.service.database.dao

import network.cow.user.service.database.table.UserMetadata
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID
import network.cow.user.service.database.dao.UserMetadata as This

/**
 * @author Benedikt Wüller
 */
class UserMetadata(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<This>(UserMetadata)
    var user by User referencedOn UserMetadata.user
    var username by UserMetadata.username
    var locale by UserMetadata.locale
}
