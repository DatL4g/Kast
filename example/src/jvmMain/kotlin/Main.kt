import dev.datlag.kast.Kast
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) {
    Kast.restartDiscovery()

    runBlocking {
        Kast.allAvailableDevices.collect { list ->
            list.forEach {
                println(it.name)
                println(it.type)
            }
        }
    }
}