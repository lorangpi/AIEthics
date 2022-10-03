import java.util.Arrays;
import java.util.List;

import com.supermarket.*;
import com.supermarket.SupermarketObservation.Player;
import com.supermarket.SupermarketObservation.Cart;
import com.supermarket.SupermarketObservation.Shelf;

public class Agent extends SupermarketComponentImpl {


    private String goalLocation = "oranges";

    List<String> food = Arrays.asList("milk", "chocolate milk", "strawberry milk", "apples", "oranges", "banana", "strawberry", "raspberry", "sausage", "steak",
    "chicken", "ham", "brie cheese", "swiss cheese", "cheese wheel", "garlic", "leek", "red bell pepper", "carrot", "lettuce", "avocado", "broccoli", "cucumber", "yellow bell pepper");
    List<String> counters = Arrays.asList("fish", "prepared_food");
    List<String> notable_places = Arrays.asList("exit", "counter", "cart");

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

        if (this.food.contains(this.goalLocation)){
            System.out.println(Shelf_index_of(obs, this.goalLocation));
            Go_to_Shelf(obs, agent, this.goalLocation);
            
        }
        else if (this.counters.contains(this.goalLocation)){

        }
        else if (this.notable_places.contains(this.goalLocation)){

        }

        //while(!(obs.atCartReturn(agent.index))){
        //    goSouth();
        //    obs = getLastObservation();
        //    agent = obs.players[0];
        //}
        //interactWithObject();
        //interactWithObject();
        //System.out.println(goalLocation);

       // while(!(obs.inAisleHub(agent.index))){
       //     goEast();
       //     obs = getLastObservation();
       //     agent = obs.players[0];
       // }

        //while(obs.southOfExitRow(agent)){
        //    goNorth();
       //     obs = getLastObservation();
       //     agent = obs.players[0];
       // }

        //while(!(agent.left_store)){
        //    goWest();
        //    obs = getLastObservation();
        //    agent = obs.players[0];
       // }

    }


    public int Shelf_index_of(SupermarketObservation obs, String food_name){
        int counter = 0;
        for (Shelf element : obs.shelves){
            System.out.println(element.food_name);
            if (element.food_name == food_name){break;}
            else{
                counter = counter + 1;
            }
        }
        return counter;


        }

    public void Go_to_Shelf(SupermarketObservation obs, Player agent, String food_name){
        if (obs.westOf(agent, obs.shelves[0]) && !(obs.inAisleHub(agent.index))){goEast();}
        else if (obs.eastOf(agent, obs.shelves[4]) && !(obs.inRearAisleHub(agent.index))){goWest();}
        else{
            int index_shelf = Shelf_index_of(obs, this.goalLocation);
            int aisle = index_shelf / 5 + 1;
            int number = index_shelf % 5;
            if (obs.northOfAisle(agent.index, aisle)){
                goSouth();
            }
            else if (obs.southOfAisle(agent.index, aisle)){
                goNorth();
            }
            else {
                if (obs.westOf(agent, obs.shelves[number])){goEast();}
            }
        }




    }
}