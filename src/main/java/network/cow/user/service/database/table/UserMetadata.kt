package network.cow.user.service.database.table

/**
 * @author Benedikt Wüller
 */
object UserMetadata : Metadata("user_metadata") {
    val user = reference("user_id", Users).uniqueIndex()
}
