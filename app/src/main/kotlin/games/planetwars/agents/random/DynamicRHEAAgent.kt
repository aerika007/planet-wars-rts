package games.planetwars.agents.random

import games.planetwars.agents.*
import games.planetwars.core.*
import util.Vec2d
import kotlin.math.max

class DynamicRHEAAgent : PlanetWarsPlayer() {

    private lateinit var rheaAgent: RHEAAgent

    override fun prepareToPlayAs(player: Player, params: GameParams, opponent: String?): String {
        super.prepareToPlayAs(player, params, opponent)
        rheaAgent = RHEAAgent(
            sequenceLength = 25,
            useShiftBuffer = true
        )
        rheaAgent.prepareToPlayAs(player, params, opponent)
        return getAgentType()
    }

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter { it.owner == player && it.nShips > 0 }
        val enemyPlanets = gameState.planets.filter { it.owner != player && it.owner != Player.Neutral }

        if (myPlanets.isEmpty() || enemyPlanets.isEmpty()) {
            return Action.doNothing()
        }

        val myShips = myPlanets.sumOf { it.nShips }
        val oppShips = gameState.planets.filter { it.owner == player.opponent() }.sumOf { it.nShips }
        val myPlanetCount = myPlanets.size
        val oppPlanetCount = gameState.planets.count { it.owner == player.opponent() }

        val losing = myShips < oppShips * 0.8 || myPlanetCount < oppPlanetCount

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
        }

        val target = when (gamePhase) {
            "early" -> enemyPlanets.minByOrNull {
                val distance = it.position.distance(source?.position ?: Vec2d(0.0, 0.0))
                (it.nShips - it.growthRate * distance / 5)
            }
            "mid" -> enemyPlanets.minByOrNull {
                val distance = it.position.distance(source?.position ?: Vec2d(0.0, 0.0))
                (it.nShips - 2 * it.growthRate * distance / 5)
            }
            else -> enemyPlanets.minByOrNull {
                val distance = it.position.distance(source?.position ?: Vec2d(0.0, 0.0))
                (it.growthRate - it.nShips / 2 + distance / 10)
            }
        }

        val shipsToSend = when (gamePhase) {
            "early" -> source?.nShips?.times(0.4)?.coerceAtLeast(5.0)
            "mid" -> source?.nShips?.times(0.5)?.coerceAtLeast(10.0)
            else -> source?.nShips?.times(0.6)
        }

        return if (!losing && source != null && target != null && shipsToSend != null && shipsToSend > 0) {
            Action(player, source.id, target.id, shipsToSend)
        } else {
            rheaAgent.getAction(gameState)
        }
    }

    override fun getAgentType(): String = "Dynamic RHEA Phase-Aware Agent v2"
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