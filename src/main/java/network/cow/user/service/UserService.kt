package network.cow.user.service

import io.grpc.Status
import io.grpc.stub.StreamObserver
import network.cow.mooapis.user.v1.GetPlayerRequest
import network.cow.mooapis.user.v1.GetPlayerResponse
import network.cow.mooapis.user.v1.GetPlayerUserRequest
import network.cow.mooapis.user.v1.GetPlayerUserResponse
import network.cow.mooapis.user.v1.GetPlayersRequest
import network.cow.mooapis.user.v1.GetPlayersResponse
import network.cow.mooapis.user.v1.GetUserPlayersRequest
import network.cow.mooapis.user.v1.GetUserPlayersResponse
import network.cow.mooapis.user.v1.GetUserRequest
import network.cow.mooapis.user.v1.GetUserResponse
import network.cow.mooapis.user.v1.PlayerIdentifier
import network.cow.mooapis.user.v1.UserServiceGrpc
import network.cow.user.service.database.DatabaseService
import network.cow.user.service.database.dao.Player
import network.cow.user.service.database.dao.User
import network.cow.user.service.database.table.Players
import network.cow.user.service.database.table.Players.referenceId
import network.cow.user.service.database.table.Players.referenceType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import network.cow.mooapis.user.v1.Player as GrpcPlayer
import network.cow.mooapis.user.v1.User as GrpcUser

/**
 * @author Benedikt Wüller
 */
class UserService : UserServiceGrpc.UserServiceImplBase() {

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

    override fun getPlayerUser(request: GetPlayerUserRequest, responseObserver: StreamObserver<GetPlayerUserResponse>) {
        transaction (DatabaseService.database) {
            responseObserver.onNext(GetPlayerUserResponse.newBuilder().setUser(getGrpcUser(request.identifier)).build())
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

    private fun getPlayer(identifier: PlayerIdentifier) : Player {
        // Find the given player or create a new one.
        val player = Player.find {
            (referenceId eq identifier.id) and (referenceType eq identifier.type)
        }.firstOrNull() ?: Player.new {
            user = User.new {}
            referenceId = identifier.id
            referenceType = identifier.type
        }

        // If the username is set, update the player username.
        if (identifier.hasUsername()) {
            player.username = identifier.username
        }

        return player
    }

    private fun getGrpcPlayer(identifier: PlayerIdentifier) = this.mapGrpcPlayer(this.getPlayer(identifier))

    private fun mapGrpcPlayer(player: Player) : GrpcPlayer {
        return GrpcPlayer.newBuilder()
            .setId(player.id.toString())
            .setReferenceId(player.referenceId)
            .setReferenceType(player.referenceType)
            .setUsername(player.username ?: player.id.toString()) // use the uuid as username, if no username is known.
            .build()
    }

    private fun getGrpcUser(identifier: PlayerIdentifier) = this.mapGrpcUser(this.getPlayer(identifier).user)

    private fun getGrpcUser(id: UUID) = User.findById(id)?.let { this.mapGrpcUser(it) }

    private fun mapGrpcUser(user: User) : GrpcUser {
        return GrpcUser.newBuilder()
            .setId(user.id.toString())
            .build()
    }

}
