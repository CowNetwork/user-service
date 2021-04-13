package network.cow.user.service.database.table

import org.jetbrains.exposed.dao.id.UUIDTable

/**
 * @author Benedikt Wüller
 */
object Players : UUIDTable() {
    val user = reference("user", Users)
    val referenceId = varchar("reference_id", 64).index()
    val referenceType = varchar("reference_type", 32).index()
    val username = varchar("username", 32).nullable().index()
}
