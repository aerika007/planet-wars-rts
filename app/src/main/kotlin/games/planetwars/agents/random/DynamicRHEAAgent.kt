package games.planetwars.agents.random

import games.planetwars.agents.*
import games.planetwars.core.*
import kotlin.math.max
import kotlin.math.min

class DynamicRHEAAgent : PlanetWarsPlayer() {

    private lateinit var rheaAgent: RHEAAgent

    override fun prepareToPlayAs(player: Player, params: GameParams, opponent: String?): String {
        super.prepareToPlayAs(player, params, opponent)

        rheaAgent = RHEAAgent(
            sequenceLength = 20,
            useShiftBuffer = true
        )
        rheaAgent.prepareToPlayAs(player, params, opponent)

        return getAgentType()
    }

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter { it.owner == player && it.nShips > 0 }
        val enemyPlanets = gameState.planets.filter { it.owner != player && it.owner != Player.Neutral }

        if (myPlanets.isEmpty() || enemyPlanets.isEmpty()) return Action.doNothing()

        val myShips = myPlanets.sumOf { it.nShips }
        val oppShips = gameState.planets.filter { it.owner == player.opponent() }.sumOf { it.nShips }
        val myPlanetCount = myPlanets.size
        val oppPlanetCount = gameState.planets.count { it.owner == player.opponent() }

        val losing = myShips < oppShips * 0.8 || myPlanetCount < oppPlanetCount
        if (losing) {
            return rheaAgent.getAction(gameState)
        }

        val totalShips = gameState.planets.sumOf {
            when (it.owner) {
                player -> it.nShips
                player.opponent() -> -it.nShips
                else -> 0.0
            }
        }

        val gamePhase = when {
            totalShips < 200 -> "early"
            totalShips < 600 -> "mid"
            else -> "late"
        }

        val source = when (gamePhase) {
            "early" -> myPlanets.maxByOrNull { it.nShips }
            "mid" -> myPlanets.maxByOrNull { it.nShips + 2 * it.growthRate }
            else -> myPlanets.maxByOrNull { it.growthRate }
        } ?: return Action.doNothing()

        val target = when (gamePhase) {
            "early" -> enemyPlanets.minByOrNull { it.nShips - it.growthRate }
            "mid" -> enemyPlanets.minByOrNull { it.nShips - 2 * it.growthRate }
            else -> enemyPlanets.minByOrNull { it.growthRate - it.nShips / 2 }
        } ?: return Action.doNothing()

        val shipsToSend = when (gamePhase) {
            "early" -> max(5.0, source.nShips * 0.4)
            "mid" -> max(10.0, source.nShips * 0.5)
            else -> source.nShips * 0.6
        }

        return Action(player, source.id, target.id, shipsToSend)
    }

    override fun getAgentType(): String = "Dynamic RHEA Phase-Aware Agent"
}

fun main() {
    val agent = DynamicRHEAAgent()
    val params = GameParams()
    agent.prepareToPlayAs(Player.Player1, params, null)
    val gameState = GameStateFactory(params).createGame()
    val action = agent.getAction(gameState)
    println("Agent Type: ${agent.getAgentType()}")
    println("Action: $action")
}