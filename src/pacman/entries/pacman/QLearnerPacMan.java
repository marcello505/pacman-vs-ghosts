package pacman.entries.pacman;

import com.github.chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.internal.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class QLearnerPacMan extends Controller<MOVE>
{
	private MOVE myMove=MOVE.NEUTRAL;
	private int lastAction = 0;
	private int lastState = 0;
	private QLearner agent;

	public QLearnerPacMan(){
		agent = new QLearner(4096, 4);
	}
	
	public MOVE getMove(Game game, long timeDue) 
	{
		lastState = convertGameStateToInt(game);
		lastAction = agent.selectAction(lastState).getIndex();

		myMove = getMoveBasedOnActionId(lastAction);
		return myMove;
	}

	public void updateStrategy(Game newGame, double reward){
		agent.update(lastState, lastAction, convertGameStateToInt(newGame), reward);
	}

	private int convertGameStateToInt(Game game){
		int result = 0;
		int pacmanNode = game.getPacmanCurrentNodeIndex();
		int closestPillNode = game.getClosestNodeIndexFromNodeIndex(pacmanNode, game.getActivePillsIndices(), Constants.DM.MANHATTAN);


		result += getNeighborStatus(game, pacmanNode, MOVE.LEFT);
		result += getNeighborStatus(game, pacmanNode, MOVE.RIGHT) << 3;
		result += getNeighborStatus(game, pacmanNode, MOVE.UP) << 3 * 2;
		result += getNeighborStatus(game, pacmanNode, MOVE.DOWN) << 3 * 3;


		return result;
	}

	private int getNeighborStatus(Game game, int pacmanIndex, MOVE move){
		int nodeIndex = game.getNeighbour(pacmanIndex, move);
		if (nodeIndex == -1){
			return 5; //wall
		}

		final int DANGER_DISTANCE = 3;
		int inkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.INKY);
		int inkyDistance = game.getShortestPathDistance(pacmanIndex, inkyIndex);
		int blinkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.BLINKY);
		int blinkyDistance = game.getShortestPathDistance(pacmanIndex, blinkyIndex);
		int pinkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.PINKY);
		int pinkyDistance = game.getShortestPathDistance(pacmanIndex, pinkyIndex);
		int sueIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.SUE);
		int sueDistance = game.getShortestPathDistance(pacmanIndex, sueIndex);

		Node node = game.getCurrentMaze().graph[nodeIndex];

		//Get closestPill
		int[] activePills = game.getActivePillsIndices();
		int[] activePowerPills = game.getActivePowerPillsIndices();
		int[] combinedPillIndices = new int[activePills.length + activePowerPills.length];
		System.arraycopy(activePills, 0, combinedPillIndices, 0, activePills.length);
		System.arraycopy(activePowerPills, 0, combinedPillIndices, activePills.length, activePowerPills.length);
		int closestPillIndex = game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), combinedPillIndices, Constants.DM.MANHATTAN);

		if(blinkyIndex == nodeIndex
			|| inkyIndex == nodeIndex
			|| pinkyIndex == nodeIndex
			|| sueIndex == nodeIndex){
			return 4; //a g-g-g-ghost
		}
		if(inkyDistance < DANGER_DISTANCE || blinkyDistance < DANGER_DISTANCE || pinkyDistance < DANGER_DISTANCE || sueDistance < DANGER_DISTANCE){
			//Ghost is close
			if (inkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, inkyIndex, Constants.DM.PATH) == move){
				return 3;
			}
			if (blinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, blinkyIndex, Constants.DM.PATH) == move){
				return 3;
			}
			if (pinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, pinkyIndex, Constants.DM.PATH) == move){
				return 3;
			}
			if (sueDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, sueIndex, Constants.DM.PATH) == move){
				return 3;
			}
		}
		if(node.powerPillIndex >= 0 || node.pillIndex >= 0){
			return 2;//pill!!!
		}
		if(closestPillIndex != -1 && game.getNextMoveTowardsTarget(pacmanIndex, closestPillIndex, Constants.DM.MANHATTAN) == move){
			return 1;//this way for pill
		}
		return 0;//empty node
	}


	private MOVE getMoveBasedOnActionId(int actionId){
		switch (actionId){
			case 0:
				return MOVE.LEFT;
			case 1:
				return MOVE.UP;
			case 2:
				return MOVE.RIGHT;
			case 3:
				return MOVE.DOWN;
		}
		return MOVE.NEUTRAL;
	}

	public void saveModel(String path){
		String json = agent.toJson();
		try{
			FileWriter writer = new FileWriter(path);
			writer.write(json);
			writer.close();
		}
		catch (Exception e){

		}
	}


}