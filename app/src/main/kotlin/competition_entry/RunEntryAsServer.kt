package competition_entry

import games.planetwars.agents.random.CarefulRandomAgent
import games.planetwars.agents.random.DynamicRHEAAgent
import json_rmi.GameAgentServer

fun main() {
    val server = GameAgentServer(port = 8080, agentClass = DynamicRHEAAgent::class)
    server.start(wait = true)
}
