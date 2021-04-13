package network.cow.user.service.database.table

/**
 * @author Benedikt Wüller
 */
object UserMetadata : Metadata() {
    val user = reference("user", Users).uniqueIndex()
}
