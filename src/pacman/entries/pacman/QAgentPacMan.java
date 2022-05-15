package pacman.entries.pacman;

import com.github.chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import com.github.chen0040.rl.learning.qlearn.QAgent;
import pacman.game.internal.Node;

import javax.management.OperationsException;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class QAgentPacMan extends Controller<MOVE>
{
	private final int startingState = 0;
	private MOVE myMove=MOVE.NEUTRAL;
	private QAgent agent;
	private int lastScore = 0;
	private int lastAction = 0;

	public QAgentPacMan(){
		agent = new QAgent(4096, 4);
		agent.start(startingState);
	}
	
	public MOVE getMove(Game game, long timeDue) 
	{
		agent.update(lastAction, convertGameStateToInt(game, true), game.getScore() - lastScore);
		lastScore = game.getScore();

//		Set<Integer> possibleActions = getPossibleMovesBasedOnGameState(game, game.isJunction(game.getPacmanCurrentNodeIndex()));
		Set<Integer> possibleActions = getPossibleMovesBasedOnGameState(game, false);
		lastAction = agent.selectAction(possibleActions).getIndex();
		myMove = getMoveBasedOnActionId(lastAction);

		return myMove;
	}

	public void gameOver(){
		agent.update(lastAction, -1, -1000);
		lastScore = 0;
		agent.start(startingState);
	}


	private int convertGameStateToInt(Game game, boolean simpleState){
		int result = 0;
		int pacmanNode = game.getPacmanCurrentNodeIndex();

		if(simpleState){
			result += getSimpleNeighborStatus(game, MOVE.LEFT) << 2 * 0;
			result += getSimpleNeighborStatus(game, MOVE.RIGHT) << 2 * 1;
			result += getSimpleNeighborStatus(game, MOVE.UP) << 2 * 2;
			result += getSimpleNeighborStatus(game, MOVE.DOWN) << 2 * 3;

		}
		else{
			result += getNeighborStatus(game, pacmanNode, MOVE.LEFT);
			result += getNeighborStatus(game, pacmanNode, MOVE.RIGHT) << 3;
			result += getNeighborStatus(game, pacmanNode, MOVE.UP) << 3 * 2;
			result += getNeighborStatus(game, pacmanNode, MOVE.DOWN) << 3 * 3;
		}


		return result;
	}

	private int getNeighborStatus(Game game, int pacmanIndex, MOVE move){
		int nodeIndex = game.getNeighbour(pacmanIndex, move);
		if (nodeIndex == -1){
			return 6; //wall
		}

		final int DANGER_DISTANCE = 32;
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
		int closestPillIndex = game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), combinedPillIndices, Constants.DM.PATH);

		if(blinkyIndex == nodeIndex
				|| inkyIndex == nodeIndex
				|| pinkyIndex == nodeIndex
				|| sueIndex == nodeIndex){
			return 5; //a g-g-g-ghost
		}
		if((inkyDistance != -1 && inkyDistance < DANGER_DISTANCE) || (blinkyDistance != -1 && blinkyDistance < DANGER_DISTANCE) || ( pinkyDistance != -1 && pinkyDistance < DANGER_DISTANCE) || ( sueDistance != -1 && sueDistance < DANGER_DISTANCE)){
			//Ghost is close
			if (inkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, inkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.INKY) > 0) return 4;
				else return 3;
			}
			if (blinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, blinkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.BLINKY) > 0) return 4;
				else return 3;
			}
			if (pinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, pinkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.PINKY) > 0) return 4;
				else return 3;
			}
			if (sueDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, sueIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.SUE) > 0) return 4;
				else return 3;
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

	private int getSimpleNeighborStatus(Game game, MOVE move){
		int pacmanIndex = game.getPacmanCurrentNodeIndex();
		int nodeIndex = game.getNeighbour(pacmanIndex, move);
		if(nodeIndex == -1){
			return 0; //Wall
		}

		//Get ghost info
		final int DANGER_DISTANCE = 32;
		int inkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.INKY);
		int inkyDistance = game.getShortestPathDistance(pacmanIndex, inkyIndex);
		int blinkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.BLINKY);
		int blinkyDistance = game.getShortestPathDistance(pacmanIndex, blinkyIndex);
		int pinkyIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.PINKY);
		int pinkyDistance = game.getShortestPathDistance(pacmanIndex, pinkyIndex);
		int sueIndex = game.getGhostCurrentNodeIndex(Constants.GHOST.SUE);
		int sueDistance = game.getShortestPathDistance(pacmanIndex, sueIndex);

		//Get closestPill
		int[] activePills = game.getActivePillsIndices();
		int[] activePowerPills = game.getActivePowerPillsIndices();
		int[] combinedPillIndices = new int[activePills.length + activePowerPills.length];
		System.arraycopy(activePills, 0, combinedPillIndices, 0, activePills.length);
		System.arraycopy(activePowerPills, 0, combinedPillIndices, activePills.length, activePowerPills.length);
		int closestPillIndex = game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), combinedPillIndices, Constants.DM.PATH);

		if((inkyDistance != -1 && inkyDistance < DANGER_DISTANCE) || (blinkyDistance != -1 && blinkyDistance < DANGER_DISTANCE) || ( pinkyDistance != -1 && pinkyDistance < DANGER_DISTANCE) || ( sueDistance != -1 && sueDistance < DANGER_DISTANCE)){
			//Ghost is close
			boolean normalGhost = false;
			boolean edibleGhost = false;
			if (inkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, inkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.INKY) > 0) edibleGhost = true;
				else normalGhost = true;
			}
			if (blinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, blinkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.BLINKY) > 0) edibleGhost = true;
				else normalGhost = true;
			}
			if (pinkyDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, pinkyIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.PINKY) > 0) edibleGhost = true;
				else normalGhost = true;
			}
			if (sueDistance < DANGER_DISTANCE && game.getNextMoveTowardsTarget(pacmanIndex, sueIndex, Constants.DM.PATH) == move){
				if(game.getGhostEdibleTime(Constants.GHOST.SUE) > 0) edibleGhost = true;
				else normalGhost = true;
			}

			if(normalGhost) return 3; //normalGhost is close
			else if(edibleGhost) return 2; //edibleGhost is close
		}
		if(closestPillIndex != -1 && game.getNextMoveTowardsTarget(pacmanIndex, closestPillIndex, Constants.DM.PATH) == move){
			return 1;//this way for pill
		}

		return 0; //Nothing
	}

	private MOVE getMoveBasedOnActionId(int actionId) {
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
		//Discourage -1 actions
		lastScore += 100;
		return MOVE.NEUTRAL;
	}

	private Set<Integer> getPossibleMovesBasedOnGameState(Game game, boolean canMoveBack){
		MOVE[] gamePossibleMoves;
		if(!canMoveBack) gamePossibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
		else gamePossibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
		HashSet<Integer> result = new HashSet<Integer>();

		for (MOVE move : gamePossibleMoves) {
			switch (move){
				case LEFT -> result.add(0);
				case UP -> result.add(1);
				case RIGHT -> result.add(2);
				case DOWN -> result.add(3);
			}
		}

		return result;
	}

	public void saveModel(String path){
		String json = agent.getLearner().toJson();
		try{
			FileWriter writer = new FileWriter(path);
			writer.write(json);
			writer.close();
		}
		catch (Exception e){

		}
	}

	public void loadModel(String path){
		try{
			File myObj = new File(path);
			Scanner myReader = new Scanner(myObj);
			String json = "";
			while (myReader.hasNextLine()) {
				json = myReader.nextLine();
			}
			myReader.close();
			this.agent.setLearner(QLearner.fromJson(json));
		}
		catch (Exception e){
			System.out.println(e);

		}
	}


}