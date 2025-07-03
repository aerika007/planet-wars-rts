package games.planetwars.agents.random

import games.planetwars.agents.Action
import games.planetwars.agents.PartialObservationAgent
import games.planetwars.core.*
import kotlin.math.max

class PartialRHEAAgent : PartialObservationAgent {

    private lateinit var player: Player
    private lateinit var params: GameParams

    override fun prepareToPlayAs(player: Player, params: GameParams, opponent: Player?): PartialObservationAgent {
        this.player = player
        this.params = params
        return this
    }

    override fun getAction(observation: Observation): Action {
        val myPlanets = observation.observedPlanets.filter {
            it.owner == player && (it.nShips ?: 0.0) > 0
        }
        val enemyPlanets = observation.observedPlanets.filter {
            it.owner != player && it.owner != Player.Neutral
        }

        if (myPlanets.isEmpty() || enemyPlanets.isEmpty()) return Action.doNothing()

        val totalShips = observation.observedPlanets.sumOf {
            when (it.owner) {
                player -> it.nShips ?: 0.0
                player.opponent() -> -(it.nShips ?: 0.0)
                else -> 0.0
            }
        }

        val gamePhase = when {
            totalShips < 200 -> "early"
            totalShips < 600 -> "mid"
            else -> "late"
        }

        val source = when (gamePhase) {
            "early" -> myPlanets.maxByOrNull { it.nShips ?: 0.0 }
            "mid" -> myPlanets.maxByOrNull { (it.nShips ?: 0.0) + 2 * it.growthRate }
            else -> myPlanets.maxByOrNull { it.growthRate }
        } ?: return Action.doNothing()

        val target = when (gamePhase) {
            "early" -> enemyPlanets.minByOrNull { (it.nShips ?: Double.MAX_VALUE) - it.growthRate }
            "mid" -> enemyPlanets.minByOrNull { (it.nShips ?: Double.MAX_VALUE) - 2 * it.growthRate }
            else -> enemyPlanets.minByOrNull { it.growthRate - (it.nShips ?: 0.0) / 2 }
        } ?: return Action.doNothing()

        val shipsToSend = when (gamePhase) {
            "early" -> max(5.0, (source.nShips ?: 0.0) * 0.4)
            "mid" -> max(10.0, (source.nShips ?: 0.0) * 0.5)
            else -> (source.nShips ?: 0.0) * 0.6
        }

        return Action(player, source.id, target.id, shipsToSend)
    }

    override fun getAgentType(): String = "Partial RHEA (Phase-Aware) Agent"
}
