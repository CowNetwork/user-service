package network.cow.user.service.database

import network.cow.user.service.database.table.PlayerMetadata
import network.cow.user.service.database.table.Players
import network.cow.user.service.database.table.UserMetadata
import network.cow.user.service.database.table.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @author Benedikt Wüller
 */
object DatabaseService {

    val database: Database

    init {
        val host = System.getenv("POSTGRES_HOST") ?: "127.0.0.1:5432"
        val db = System.getenv("POSTGRES_DB") ?: "postgres"
        val username = System.getenv("POSTGRES_USERNAME") ?: "postgres"
        val password = System.getenv("POSTGRES_PASSWORD") ?: "postgres"

        this.database = Database.connect("jdbc:postgresql://$host/$db", "org.postgresql.Driver", username, password)

        transaction (this.database) {
            // Make sure the tables exist.
            SchemaUtils.create(Users, Players, UserMetadata, PlayerMetadata)
        }
    }

}
