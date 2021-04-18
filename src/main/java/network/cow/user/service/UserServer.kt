package network.cow.user.service

import io.grpc.ServerBuilder

/**
 * @author Benedikt Wüller
 */
class UserServer(private val port: Int) {

    private val server = ServerBuilder.forPort(port).addService(UserService()).build()

    fun start() {
        server.start()
        println("Server started, listening on port $port.")
        Runtime.getRuntime().addShutdownHook(Thread(this::stop))
    }

    fun stop() {
        println("Stopping server...")
        server.shutdown()
        println("Server stopped.")
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

}

fun main() {
    val port = System.getenv("USER_SERVICE_PORT")?.toInt() ?: System.getenv("PORT")?.toInt() ?: 5816
    val server = UserServer(port)
    server.start()
    server.blockUntilShutdown()
}
