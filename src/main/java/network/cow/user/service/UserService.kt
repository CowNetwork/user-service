package network.cow.user.service

import io.grpc.Status
import io.grpc.stub.StreamObserver
import network.cow.mooapis.user.v1.GetPlayerByIdRequest
import network.cow.mooapis.user.v1.GetPlayerByIdResponse
import network.cow.mooapis.user.v1.GetPlayerByNameRequest
import network.cow.mooapis.user.v1.GetPlayerByNameResponse
import network.cow.mooapis.user.v1.GetPlayerRequest
import network.cow.mooapis.user.v1.GetPlayerResponse
import network.cow.mooapis.user.v1.GetPlayersByIdRequest
import network.cow.mooapis.user.v1.GetPlayersByIdResponse
import network.cow.mooapis.user.v1.GetPlayersRequest
import network.cow.mooapis.user.v1.GetPlayersResponse
import network.cow.mooapis.user.v1.GetUserByNameRequest
import network.cow.mooapis.user.v1.GetUserByNameResponse
import network.cow.mooapis.user.v1.GetUserPlayersRequest
import network.cow.mooapis.user.v1.GetUserPlayersResponse
import network.cow.mooapis.user.v1.GetUserRequest
import network.cow.mooapis.user.v1.GetUserResponse
import network.cow.mooapis.user.v1.PlayerIdentifier
import network.cow.mooapis.user.v1.PlayerMetadataUpdatedEvent
import network.cow.mooapis.user.v1.UpdatePlayerMetadataRequest
import network.cow.mooapis.user.v1.UpdatePlayerMetadataResponse
import network.cow.mooapis.user.v1.UpdateUserMetadataRequest
import network.cow.mooapis.user.v1.UpdateUserMetadataResponse
import network.cow.mooapis.user.v1.UserMetadataUpdatedEvent
import network.cow.mooapis.user.v1.UserServiceGrpc
import network.cow.user.service.database.DatabaseService
import network.cow.user.service.database.dao.Player
import network.cow.user.service.database.dao.PlayerMetadata
import network.cow.user.service.database.dao.User
import network.cow.user.service.database.dao.UserMetadata
import network.cow.user.service.database.table.Players
import network.cow.user.service.database.table.Players.referenceId
import network.cow.user.service.database.table.Players.referenceType
import network.cow.user.service.database.table.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import network.cow.mooapis.user.v1.Metadata as GrpcMetadata
import network.cow.mooapis.user.v1.Player as GrpcPlayer
import network.cow.mooapis.user.v1.User as GrpcUser
import network.cow.user.service.database.table.PlayerMetadata as PlayerMetadataTable
import network.cow.user.service.database.table.UserMetadata as UserMetadataTable

/**
 * @author Benedikt Wüller
 */
class UserService : UserServiceGrpc.UserServiceImplBase() {

    private val topic = System.getenv("USER_SERVICE_KAFKA_PRODUCER_TOPIC") ?: "cow.global.user"

    // TODO: input validation / error handling

    override fun getPlayer(request: GetPlayerRequest, responseObserver: StreamObserver<GetPlayerResponse>) {
        transaction (DatabaseService.database) {
            responseObserver.onNext(GetPlayerResponse.newBuilder().setPlayer(getGrpcPlayer(request.identifier)).build())
            responseObserver.onCompleted()
        }
    }

    override fun getPlayers(request: GetPlayersRequest, responseObserver: StreamObserver<GetPlayersResponse>) {
        transaction (DatabaseService.database) {
            val players = request.identifiersList.map { getGrpcPlayer(it) }
            responseObserver.onNext(GetPlayersResponse.newBuilder().addAllPlayers(players).build())
            responseObserver.onCompleted()
        }
    }

    override fun getUser(request: GetUserRequest, responseObserver: StreamObserver<GetUserResponse>) {
        transaction (DatabaseService.database) {
            val grpcUser = getGrpcUser(UUID.fromString(request.userId))

            if (grpcUser == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The user does not exist.").asRuntimeException())
                return@transaction
            }

            responseObserver.onNext(GetUserResponse.newBuilder().setUser(grpcUser).build())
            responseObserver.onCompleted()
        }
    }

    override fun getUserPlayers(request: GetUserPlayersRequest, responseObserver: StreamObserver<GetUserPlayersResponse>) {
        transaction (DatabaseService.database) {
            val user = User.findById(UUID.fromString(request.userId))

            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The user does not exist.").asRuntimeException())
                return@transaction
            }

            val response = GetUserPlayersResponse.newBuilder()
            Player.find { Players.user eq user.id }.forEach { response.addPlayers(mapGrpcPlayer(it)) }

            responseObserver.onNext(response.build())
            responseObserver.onCompleted()
        }
    }

    override fun getPlayerById(request: GetPlayerByIdRequest, responseObserver: StreamObserver<GetPlayerByIdResponse>) {
        transaction (DatabaseService.database) {
            val player = Player.findById(UUID.fromString(request.playerId))

            if (player == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The player does not exist.").asRuntimeException())
                return@transaction
            }

            responseObserver.onNext(GetPlayerByIdResponse.newBuilder().setPlayer(mapGrpcPlayer(player)).build())
            responseObserver.onCompleted()
        }
    }

    override fun getPlayersById(request: GetPlayersByIdRequest, responseObserver: StreamObserver<GetPlayersByIdResponse>) {
        transaction (DatabaseService.database) {
            val players = request.playerIdsList.mapNotNull { Player.findById(UUID.fromString(it)) }

            if (players.size != request.playerIdsCount) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("At least one requested player does not exist.").asRuntimeException())
                return@transaction
            }

            responseObserver.onNext(GetPlayersByIdResponse.newBuilder().addAllPlayers(players.map { mapGrpcPlayer(it) }).build())
            responseObserver.onCompleted()
        }
    }

    override fun updatePlayerMetadata(request: UpdatePlayerMetadataRequest, responseObserver: StreamObserver<UpdatePlayerMetadataResponse>) {
        transaction (DatabaseService.database) {
            val player = Player.findById(UUID.fromString(request.playerId))

            if (player == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The player does not exist.").asRuntimeException())
                return@transaction
            }

            // Remove the username for all players to restore inconsistencies.
            PlayerMetadataTable.update({ PlayerMetadataTable.username eq request.metadata.username }) {
                it[username] = null
            }

            val metadata = getMetadata(player)
            metadata.locale = request.metadata.locale
            metadata.username = request.metadata.username

            responseObserver.onNext(UpdatePlayerMetadataResponse.newBuilder().setPlayer(mapGrpcPlayer(player)).build())
            responseObserver.onCompleted()

            CloudEventProducer.send(topic, PlayerMetadataUpdatedEvent.newBuilder()
                .setPlayerId(player.id.toString())
                .setMetadata(mapGrpcMetadata(metadata))
                .build()
            )
        }
    }

    override fun updateUserMetadata(request: UpdateUserMetadataRequest, responseObserver: StreamObserver<UpdateUserMetadataResponse>) {
        transaction (DatabaseService.database) {
            val user = User.findById(UUID.fromString(request.userId))

            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The user does not exist.").asRuntimeException())
                return@transaction
            }

            // Remove the username for all players to restore inconsistencies.
            UserMetadataTable.update({ UserMetadataTable.username eq request.metadata.username }) {
                it[username] = null
            }

            val metadata = getMetadata(user)
            metadata.locale = request.metadata.locale
            metadata.username = request.metadata.username

            responseObserver.onNext(UpdateUserMetadataResponse.newBuilder().setUser(mapGrpcUser(user)).build())
            responseObserver.onCompleted()

            CloudEventProducer.send(topic, UserMetadataUpdatedEvent.newBuilder()
                .setUserId(user.id.toString())
                .setMetadata(mapGrpcMetadata(metadata))
                .build()
            )
        }
    }

    override fun getPlayerByName(request: GetPlayerByNameRequest, responseObserver: StreamObserver<GetPlayerByNameResponse>) {
        transaction (DatabaseService.database) {
            val metadata = (PlayerMetadataTable leftJoin Players)
                .slice(Players.id)
                .select { (PlayerMetadataTable.username eq request.name) and (PlayerMetadataTable.player eq Players.id) }
                .firstOrNull()

            if (metadata == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The player does not exist.").asRuntimeException())
                return@transaction
            }

            responseObserver.onNext(GetPlayerByNameResponse.newBuilder().setPlayer(mapGrpcPlayer(Player[metadata[Players.id]])).build())
            responseObserver.onCompleted()
        }
    }

    override fun getUserByName(request: GetUserByNameRequest, responseObserver: StreamObserver<GetUserByNameResponse>) {
        transaction (DatabaseService.database) {
            val metadata = (UserMetadataTable leftJoin Users)
                .slice(Users.id)
                .select { (UserMetadataTable.username eq request.name) and (UserMetadataTable.user eq Users.id) }
                .firstOrNull()

            if (metadata == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("The user does not exist.").asRuntimeException())
                return@transaction
            }

            responseObserver.onNext(GetUserByNameResponse.newBuilder().setUser(mapGrpcUser(User[metadata[Users.id]])).build())
            responseObserver.onCompleted()
        }
    }

    private fun getPlayer(identifier: PlayerIdentifier) : Player {
        // Find the given player or create a new one.
        return Player.find {
            (referenceId eq identifier.id) and (referenceType eq identifier.type)
        }.firstOrNull() ?: this.createPlayer(identifier.id, identifier.type)
    }

    private fun getMetadata(target: Player) : PlayerMetadata {
        return PlayerMetadata.find { PlayerMetadataTable.player eq target.id }.firstOrNull() ?: PlayerMetadata.new {
            player = target
            locale = getMetadata(target.user).locale
        }
    }

    private fun getMetadata(target: User) : UserMetadata {
        return UserMetadata.find { UserMetadataTable.user eq target.id }.firstOrNull() ?: UserMetadata.new { user = target }
    }

    private fun createPlayer(id: String, type: String) : Player {
        return Player.new {
            user = User.new {}
            referenceId = id
            referenceType = type
        }
    }

    private fun getGrpcPlayer(identifier: PlayerIdentifier) = this.mapGrpcPlayer(this.getPlayer(identifier))

    private fun mapGrpcPlayer(player: Player) : GrpcPlayer {
        return GrpcPlayer.newBuilder()
            .setId(player.id.toString())
            .setReferenceId(player.referenceId)
            .setReferenceType(player.referenceType)
            .setUserId(player.user.id.toString())
            .setMetadata(this.mapGrpcMetadata(this.getMetadata(player)))
            .build()
    }

    private fun mapGrpcMetadata(metadata: PlayerMetadata) : GrpcMetadata {
        val builder = GrpcMetadata.newBuilder().setLocale(metadata.locale)
        metadata.username?.let { builder.setUsername(it) }
        return builder.build()
    }

    private fun mapGrpcMetadata(metadata: UserMetadata) : GrpcMetadata {
        val builder = GrpcMetadata.newBuilder().setLocale(metadata.locale)
        metadata.username?.let { builder.setUsername(it) }
        return builder.build()
    }

    private fun getGrpcUser(id: UUID) = User.findById(id)?.let { this.mapGrpcUser(it) }

    private fun mapGrpcUser(user: User) : GrpcUser {
        return GrpcUser.newBuilder()
            .setId(user.id.toString())
            .setMetadata(this.mapGrpcMetadata(this.getMetadata(user)))
            .build()
    }

}
