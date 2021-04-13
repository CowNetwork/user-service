package network.cow.user.service.database.table

/**
 * @author Benedikt Wüller
 */
object PlayerMetadata : Metadata() {
    val player = reference("player", Players).uniqueIndex()
}
