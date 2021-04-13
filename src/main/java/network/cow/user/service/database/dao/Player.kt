package network.cow.user.service.database.dao

import network.cow.user.service.database.table.Players
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

/**
 * @author Benedikt Wüller
 */
class Player(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Player>(Players)

    var user by User referencedOn Players.user
    var referenceId by Players.referenceId
    var referenceType by Players.referenceType
    var username by Players.username
}
