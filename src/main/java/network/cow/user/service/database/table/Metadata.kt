package network.cow.user.service.database.table

import org.jetbrains.exposed.dao.id.UUIDTable

/**
 * @author Benedikt Wüller
 */
abstract class Metadata : UUIDTable() {
    val username = varchar("username", 64).nullable().index()
    val locale = varchar("locale", 5).default("en_US")
}
