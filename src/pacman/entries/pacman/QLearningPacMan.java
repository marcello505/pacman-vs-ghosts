package pacman.entries.pacman;

import com.github.chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import com.github.chen0040.rl.learning.qlearn.QAgent;
import pacman.game.internal.Node;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class QLearningPacMan extends Controller<MOVE>
{
	private MOVE myMove=MOVE.NEUTRAL;
	private QAgent agent;

	public QLearningPacMan(){
		agent = new QAgent(100, 5);
		agent.start(0);
	}
	
	public MOVE getMove(Game game, long timeDue) 
	{
		agent.update(agent.getPrevAction(), convertGameStateToInt(game), game.getScore());

		myMove = getMoveBasedOnActionId(agent.selectAction().getIndex());

		return myMove;
	}

	private int convertGameStateToInt(Game game){
		int result = 0;

		Node[] graph = game.getCurrentMaze().graph;

		for(Node node : graph){
			int nodeStatus = 0;
			if(game.getGhostCurrentNodeIndex(Constants.GHOST.BLINKY) == node.nodeIndex
			|| game.getGhostCurrentNodeIndex(Constants.GHOST.INKY) == node.nodeIndex
			|| game.getGhostCurrentNodeIndex(Constants.GHOST.PINKY) == node.nodeIndex
			|| game.getGhostCurrentNodeIndex(Constants.GHOST.SUE) == node.nodeIndex){
				nodeStatus = 4; //ghost on tile
			}
			else if(game.getPacmanCurrentNodeIndex() == node.nodeIndex){
				nodeStatus = 3; //pacman on tile
			}
			else if(node.powerPillIndex > -1){
				nodeStatus = 2; //powerpill on tile
			}
			else if(node.pillIndex > -1){
				nodeStatus = 1; //pill on tile
			}
			else{
				nodeStatus = 0;
			}
			result = result * 3 + nodeStatus;
		}
		return result;
	}

	private MOVE getMoveBasedOnActionId(int actionId){
		switch (actionId){
			case 0:
				return MOVE.NEUTRAL;
			case 1:
				return MOVE.LEFT;
			case 2:
				return MOVE.UP;
			case 3:
				return MOVE.RIGHT;
			case 4:
				return MOVE.DOWN;
		}
		return MOVE.NEUTRAL;
	}


}