package network.cow.user.service.database.table

/**
 * @author Benedikt Wüller
 */
object PlayerMetadata : Metadata("player_metadata") {
    val player = reference("player_id", Players).uniqueIndex()
}
