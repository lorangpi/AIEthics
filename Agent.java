import com.supermarket.*;
import com.supermarket.SupermarketObservation.Player;
import com.supermarket.SupermarketObservation.Cart;

public class Agent extends SupermarketComponentImpl {
    private String goal_location = "apples";

    public Agent() {
        super();
        shouldRunExecutionLoop = true;
    }


    @Override
    protected void executionLoop() {
        // this is called every 100ms
        // put your code in here, e.g.

        SupermarketObservation obs = getLastObservation();
        Player agent = obs.players[0];
        while(!(obs.atCartReturn(agent.index))){
            goSouth();
            obs = getLastObservation();
            agent = obs.players[0];
        }
        interactWithObject();
        interactWithObject();
        System.out.println(goal_location);

        while(!(obs.inAisleHub(agent.index))){
            goEast();
            obs = getLastObservation();
            agent = obs.players[0];
        }

        while(obs.southOfExitRow(agent)){
            goNorth();
            obs = getLastObservation();
            agent = obs.players[0];
        }

        while(!(agent.left_store)){
            goWest();
            obs = getLastObservation();
            agent = obs.players[0];
        }

    }
}