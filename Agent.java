
/*
Pierrick Lorang & Brennan Miller-Klugman
11/27/22
Shopping Agent #2

Our proposed solution is closer to a cognitive architecture in that it has three different layers. There is an STRIPS encoded domain in HDDL format and the problem 
formulation for a planner layer that allows reasoning about the known states and actions in the domain. The planner is hierarchical, so it orders the tasks, methods 
and operators by building a hierarchical task network. Some predicates defining the initial state are assumed, others are observed, and the goal definition is changed 
each time a new instance of supermakert is created.

This planner communicates with a lower level control using a mapping between the operators in the plan and the control functions. The control layer is responsible for 
the classic supermarket functions and itself communicates with the lower actuator/sensor layer by giving commands. There is a return status from the sensor to the 
control layer, which gives information about the execution status of the command and is used to move to the next action in the plan when it is completed.

Most norms are formulated implicitly at either the plan level, the control layer level, or both. A sequence buffer ensures that the agent returns to its own cart, 
which might have weaknesses at some point; we want to improve that for later (add a checker). Random navigation helps reduce the likelihood that our agents will 
collide with each other. In cases where there is a chance of collision, a checker (ethical governor like, cf. Arkin architecture) anticipates it and guides our agents accordingly. All norms 
are referenced once in the file by name with a small explanation (they are not mentioned in every line of implicit programing).

There is no feedback from the control layer to the planning layer in case of failure (or better, anticipated norm violation) yet, but this is what we plan for the 
future and the last task: to trigger replanning in case of an anticipated norm violation (improving the ethical governor).

Referances:
    - Java API https://docs.oracle.com/javase/7/docs/api/
    - W3 schools https://www.w3schools.com/java/ (to brush up on how to use Array Lists and random generators)
    - Lilotane HTN planner https://github.com/domschrei/lilotane
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.Random;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.supermarket.*;
import com.supermarket.SupermarketObservation.Player;
import com.supermarket.SupermarketObservation.Cart;
import com.supermarket.SupermarketObservation.Shelf;
import com.supermarket.SupermarketObservation.Counter;
import com.supermarket.SupermarketObservation.Basket;
import com.supermarket.SupermarketObservation.Register;

public class Agent extends SupermarketComponentImpl {

    private String goalLocation = "strawberry milk";
    private String partialPlan = "";
    private ArrayList<String> Plan = new ArrayList<>();
    private boolean Flag = false;
    private boolean Next_action = false;
    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    private String action = "";
    private boolean Husle = false;
    private ArrayList<String> sequence = new ArrayList<String>(); // used to keep track of a sequence of actions, this can be reversed after an objective is completed to return to aisleHub
    private Random rand = new Random(); // random generator
    private int direction = -1;
    private int counter = -1; // counter used to track of the number of times the player has moved south, defaults to -1
    private List<Integer> aisleHistory = new ArrayList<Integer>(); // keeps track of previously entered aisles, used to determine if the player should move south
    private boolean listPopulated = false; // boolean to track if shopping list has been populated
    private Player agent;
    private String plan_name;
    private String init_name;
    private Boolean RePlan = false;
    private Boolean hasCart = false;
    private Boolean seedSet = false;
    private Boolean freeAisleHub, freeRearAisleHub = true;
    List<String> food = new ArrayList<String>();
    List<String> counters = new ArrayList<String>();
    List<String> notable_places = Arrays.asList("exit", "register", "cart");

    public Agent() {
        super();
        shouldRunExecutionLoop = true;

    }

    @Override
    protected void executionLoop() {
        // this is called every 100ms
        // put your code in here, e.g.

        SupermarketObservation obs = getLastObservation();
        this.agent = obs.players[this.playerIndex];

        checkAisleHub(obs); // update aislehub oberservation

        if (!this.seedSet) { // make sure random seed is set
            this.rand.setSeed(System.currentTimeMillis() * (long) this.playerIndex);
            this.seedSet = true;
        }

        this.plan_name = "Plan" + String.valueOf(this.playerIndex) + ".txt";
        this.init_name = "shopping" + String.valueOf(this.playerIndex) + ".hddl";

        if (!this.listPopulated) { // populate list
            populateList(obs);
        }

        if (this.partialPlan.length() == 0) {
            this.partialPlan = Get_initial_state(obs);
        }

        if (this.Plan.size() == 0 && !(this.Flag)) {
            Get_plan(obs);
            this.Flag = true;
            this.Plan = Read_plan();
            Next_action = true;
        }

        if (this.Flag) {

            if (Next_action) {
                this.action = this.Plan.remove(0);
                Dictionary action_dict = CollectionDictionary.main(action.replaceAll("\\d", "").split(" "));
                System.out.println("\n\tNext action::");
                System.out.println(action_dict);
                Next_action = false;
            }
            // Dictionary action_dict = CollectionDictionary.main(action.split(" "));
            Dictionary action_dict = CollectionDictionary.main(action.replaceAll("\\d", "").split(" "));

            // System.out.println(action_dict.get(1));
            if (((String) action_dict.get(1)).contains("navigate")) {
                Next_action = false;
                if (((String) action_dict.get(4)).contains("pickup")) {
                    this.goalLocation = "cart";
                } else if (((String) action_dict.get(4)).contains("counter")) {
                    this.goalLocation = "register";
                } else {
                    this.goalLocation = (String) ((String) action_dict.get(4)).replace("_", " ");
                    ;
                }
                // System.out.println(this.goalLocation);

                if (this.food.contains(this.goalLocation)) {
                    Go_to_Shelf(obs, this.goalLocation);
                    if (RePlan && this.Plan.size() >= 20) {
                        if (this.action.contains("navigate")) {
                            this.Plan.add(0, this.action);
                            // System.out.println(this.Plan);
                            Collections.swap(this.Plan, 0, 7);
                            Collections.swap(this.Plan, 1, 8);
                            Collections.swap(this.Plan, 2, 9);
                            Collections.swap(this.Plan, 3, 10);
                            Collections.swap(this.Plan, 4, 11);
                            Collections.swap(this.Plan, 5, 12);
                            Collections.swap(this.Plan, 6, 13);
                        }
                    }
                } else if (this.counters.contains(this.goalLocation)) {
                    Go_to_Counter(obs, this.goalLocation);
                } else if (this.notable_places.contains(this.goalLocation)) {
                    Go_to(obs, this.goalLocation);
                }
            } else if (((String) action_dict.get(1)).contains("release_cart")) {
                action("toggle", true, obs);
                this.hasCart = false;
                Next_action = true;
                System.out.println("\n\tRelease::");
            } else if (((String) action_dict.get(1)).contains("grab_cart_random_move")) { // PlayerCollisionNorm & ObjectCollisionNorm: function to move south randomly after retrieving cart, this is used so that players will enter the aisle towards the north and exit towards the south. The
                                                                                          // randomness should help prevent collisions
                int size = this.aisleHistory.size();
                if (this.counter == -1) {
                    this.counter = 6;
                } else if (this.counter != 0
                        && ((size < 2) || (this.aisleHistory.get(size - 1) != this.aisleHistory.get(size - 2)))) { // move south before exiting an aisle, this should help to prevent collisions between an agent entering and an agent leaving an aisle
                    action("south", false, obs);
                    this.counter -= 1;
                } else {
                    this.counter = -1;
                    Next_action = true;
                }
            } else if (((String) action_dict.get(1)).contains("grab_cart_exit")) {
                Next_action = true;
            } else if (((String) action_dict.get(1)).contains("buy")) {
                this.goalLocation = "register";
                Go_to(obs, this.goalLocation);
                interactWithObject();
                // interactWithObject(); //erase comment to display final bought list in the shopping environment
                Next_action = true;
            } else if (((String) action_dict.get(1)).contains("face_cart")) {
                this.goalLocation = (String) ((String) action_dict.get(3)).replace("_", " ");
                returnToCart(obs);

                // Next_action = true;
            } else if (((String) action_dict.get(1)).contains("face_object")) {
                this.goalLocation = (String) ((String) action_dict.get(3)).replace("_", " ");
                if (this.goalLocation.contains("counter")) {
                    this.goalLocation = "register";
                }
                if (((String) action_dict.get(4)).contains("pickup")) {
                    this.goalLocation = "cart";
                    Next_action = true;
                }
                int index_shelf = Shelf_index_of(obs, this.goalLocation);
                if (this.food.contains(this.goalLocation)) {
                    Face_Shelf(obs, this.goalLocation);
                } else if (this.counters.contains(this.goalLocation)) {
                    Face_Counter(obs, this.goalLocation);
                } else if (this.notable_places.contains(this.goalLocation)) {
                    Face(obs, this.goalLocation);
                } else {
                    Next_action = true;
                }
            } else if (((String) action_dict.get(1)).contains("get_cart")) {
                this.goalLocation = "cart";
                Go_to(obs, this.goalLocation);
                interactWithObject();
                interactWithObject();
                this.hasCart = true;
                Next_action = true;
            } else if (!(((String) action_dict.get(1)).contains("nop")) || !(action_dict.isEmpty())) {
                interactWithObject();
                interactWithObject();// InteractionCancellationNorm: Several norms are implicitely defined in the planner i.e. are ordered by the planning actions and can't occur otherwise. It's the case of the interactions.
                Next_action = true;
            } else {
            }
        }
    }

    public void checkAisleHub(SupermarketObservation obs) {
        this.freeAisleHub = true;
        this.freeRearAisleHub = true;

        for (int i = 0; i < obs.players.length; i++) {
            if (i != this.agent.index) {
                if (obs.inAisleHub(i)) {
                    System.out.println("Aisle Hub");

                    this.freeAisleHub = false;
                } else if (obs.inRearAisleHub(i)) {
                    System.out.println("Rear Aisle Hub");
                    this.freeRearAisleHub = false;
                }
            }
        }
    }

    public void populateList(SupermarketObservation obs) { // function used to populate the list array list based off the shopping list
        for (int i = 0; i < obs.shelves.length; i++) {
            if (!this.food.contains(obs.shelves[i].food_name)) {
                this.food.add(obs.shelves[i].food_name);
            }
        }
        for (int i = 0; i < obs.counters.length; i++) {
            if (!this.counters.contains(obs.counters[i].food)) {
                this.counters.add(obs.counters[i].food);
            }
        }
        this.listPopulated = true;
    }

    public static class CollectionDictionary {
        public static Dictionary main(String[] args) {
            ArrayList<String> list = new ArrayList<String>();
            for (String elem : args) {
                list.add(elem);
            }
            Dictionary dictionary = new Hashtable();
            Hashtable<Integer, String> hashTable = new Hashtable<Integer, String>();
            for (int i = 0; i < args.length; i++) {
                hashTable.put(i + 1, list.get(i));
            }
            return hashTable;
        }
    }

    public String Get_initial_state(com.supermarket.SupermarketObservation obs) {
        // Method that returns the partial plan from the observation in a HDDL file
        String[] shopping_list = obs.players[this.playerIndex].shopping_list;
        int[] shopping_quant = obs.players[this.playerIndex].list_quant;
        String object;
        String objects = "";
        String init_state = "";
        String buffer = "";

        int list_counter = 0;

        try {
            Files.copy(new File("shopping.hddl").toPath(), new File(this.init_name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
        }

        // for loop
        for (String iterator : shopping_list) {
            object = iterator.replace(" ", "_");
            for (int item_counter = 0; item_counter < shopping_quant[list_counter]; item_counter++) {
                object += item_counter;
                objects += object + " ";
                if (buffer != "") {
                    init_state += "(on " + buffer + " " + object + ") ";
                } else {
                    init_state += "(clear " + object + ") ";
                }
                buffer = object;
            }
            list_counter += 1;
        }
        objects += "- food";
        init_state += "(on " + buffer + " list) ";
        System.out.println(init_state);
        // Instantiating the File class
        String filePath = this.init_name;
        // Instantiating the Scanner class to read the file
        try {
            Path path = Paths.get(init_name);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(7, objects);
            lines.set(19, init_state);
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
        }
        return init_state;
    }

    public void Get_plan(com.supermarket.SupermarketObservation obs) {
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)

        String[] shopping_list = obs.players[this.playerIndex].shopping_list;
        System.out.println("\n--------------------------- The shopping list is: ----------------------------\n");
        System.out.println(Arrays.toString(obs.players[this.playerIndex].shopping_list));
        System.out.println(Arrays.toString(obs.players[this.playerIndex].list_quant));

        List<String> intermediate_plan = new ArrayList<String>();

        try {
            String[] args = new String[] { "/bin/bash", "-c",
                    "./lilotane domain.hddl " + this.init_name + " -v=0 | cut -d' ' -f2- | sed 1,2d | head -n -2 > "
                            + this.plan_name };
            Process proc = new ProcessBuilder(args).start();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException ex) {
        }
        System.out.println("Plan saved");
        return;
    }

    public String Get_action(ArrayList<String> Plan) {
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)
        String a = "";
        return a;
    }

    public ArrayList<String> Read_plan() {
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)

        String[] lines = {};
        List<String> grp = new ArrayList<String>();

        try {

            File f = new File(this.plan_name);

            BufferedReader b = new BufferedReader(new FileReader(f));

            String readLine = "";

            System.out.println("\n----------------------------- The Plan is: ------------------------------\n");

            while ((readLine = b.readLine()) != null) {
                if (isNumeric(Arrays.asList(readLine.split(" ", 2)).get(0))) {
                    break;
                }
                System.out.println(readLine);
                grp.add(readLine);
            }
            System.out.println("\n-------------------------------------------------------------------------\n");
            System.out.println("\n---------------------------------Start-----------------------------------\n");

        } catch (IOException ex) {
        }
        return (ArrayList<String>) grp;
    }

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }

    public int Shelf_index_of(SupermarketObservation obs, String food_name) {
        // Method that returns the index of the Shelf of the aimed food
        int counter = 0;
        for (Shelf element : obs.shelves) {
            // System.out.println(element.food_name);
            if (element.food_name.equals(food_name)) {
                break;
            } else {
                counter = counter + 1;
            }
        }
        return counter;
    }

    public int Counter_index_of(SupermarketObservation obs, String food_name) {
        // Method that returns the index of the Counter of the aimed food
        int counter = 0;
        for (Counter element : obs.counters) {
            if (element.food.equals(food_name)) {
                break;
            } else {
                counter = counter + 1;
            }
        }
        return counter;
    }

    public void action(String movement, boolean append, SupermarketObservation obs) {
        /*
         * The action function is used to call player commands like moveSouth, this function has the added benifit of being
         * able to keep track of previous actions in the sequence array. This allows previous actions to be reversed after retrieving an item.
         * The append boolean is used to determine whether an action should be added to the sequence or not, as some actions should not be memorized
         */

        // check movement type and perform action, if append, add inverted movement to sequence, this will allow the sequence to be performed in reverse later

        if (movement.equalsIgnoreCase("north")) {
            goNorth();
            if (append)
                this.sequence.add("south");
        } else if (movement.equalsIgnoreCase("south")) {
            goSouth();
            if (append)
                this.sequence.add("north");
        } else if (movement.equalsIgnoreCase("east")) {
            goEast();
            if (append)
                this.sequence.add("west");
        } else if (movement.equalsIgnoreCase("west")) {
            goWest();
            if (append)
                this.sequence.add("east");
        } else if (movement.equalsIgnoreCase("interact")) {
            interactWithObject();
            interactWithObject();
            if (append)
                this.sequence.add("interact");
        } else if (movement.equalsIgnoreCase("toggle")) { // used to pick up and drop shopping cart
            toggleShoppingCart();

            this.sequence.add("toggle"); // pickup cart
            this.sequence.add("interact"); // put any items that were picked up inside of the cart

            int dir = obs.players[this.playerIndex].direction; // get direction of cart and append to sequence
            if (dir == 0)
                this.sequence.add("north");
            else if (dir == 1)
                this.sequence.add("south");
            else if (dir == 2)
                this.sequence.add("east");
            else if (dir == 3)
                this.sequence.add("west");

        } else {
            System.out.println("found invalid move direction");
        }

    }

    public void returnToCart(SupermarketObservation obs) {
        /*
         * return to aisle hub function is used to reverse the sequence of actions.
         * This function starts at the end of the sequence arraylist and
         * performs the inverse of every action taken since leaving the cart
         * at the end of this sequence, the player will have retrieved the cart
         * 
         * CartTheftNorm: The player will always return to the cart they dropped off
         * UnattendedCartNorm: Implicit in the go_to functions, the player will never move far away from the carts as they navigate to the desired counter / shelf before dropping the cart
         */
        int size = this.sequence.size();
        if (size > 0) { // repeat previous steps
            if (!this.sequence.get(size - 1).contains("toggle")) { // check if the next action is to toggle cart, if not proceed
                action(sequence.get(size - 1), false, obs);
                this.sequence.remove(size - 1);
            } else { // sequence is complete, pickup cart
                action("toggle", false, obs);
                this.sequence.clear(); // clear arraylist
                this.hasCart = true;
                Next_action = true;
            }
        } else {
            System.out.println("Error: sequence was empty");
        }
    }

    public void Go_to_Shelf(SupermarketObservation obs, String food_name) {
        // Method that guides the agent toward the aimed Shelf

        // WrongShelfNorm: The agent won't put food at a wrong shelf as it follows a strict plan and can't interact with a shelf if it is holding an item in the domain

        int index_shelf = Shelf_index_of(obs, this.goalLocation);
        Shelf shelf = obs.shelves[index_shelf];
        int aisle = index_shelf / 5 + 1;
        boolean in_aisle = (!(obs.southOfAisle(this.agent.index, aisle))
                && !(obs.northOfAisle(this.agent.index, aisle)));

        if (this.direction == -1) { // randomly decide to prioritize east or west movement
            this.direction = this.rand.nextInt(2);
        }

        if (in_aisle) { // if in aisle move east / west to shelf
            if (obs.westOf(this.agent, shelf)) {
                action("east", true, obs);
            } else if (obs.eastOf(this.agent, shelf)) {
                action("west", true, obs);
            }

        } else if (obs.inAisleHub(this.agent.index) || obs.inRearAisleHub(this.agent.index)) { // if in aisle hub or rear aisle hub, move north or south to desired item
            if (obs.northOfAisle(this.agent.index, aisle)) {
                action("south", false, obs);
            } else if (obs.southOfAisle(this.agent.index, aisle)) {
                action("north", false, obs);
            }
        } else {

            if (this.direction == 0) { // PlayerCollisionNorm & ObjectCollisionNorm: randomly decide to go east or west if player is already in aisle, this should help to lower the chance of agent collisions

                if (this.agent.position[0] <= (obs.shelves[0].position[0] - 3)) { // prioritize east
                    if (freeAisleHub) {
                        action("east", false, obs);
                    }
                } else if (this.agent.position[0] >= (obs.shelves[4].position[0] + 3)) { // move west if must
                    if (freeRearAisleHub) {
                        action("west", false, obs);
                    }
                } else {
                    if (freeAisleHub) {
                        action("east", false, obs);
                    }
                }
            } else {
                if (this.agent.position[0] >= (obs.shelves[4].position[0] + 3)) { // prioritize west
                    if (freeRearAisleHub) {
                        action("west", false, obs);
                    }
                } else if (this.agent.position[0] <= (obs.shelves[0].position[0] - 3)) { // move east if must
                    if (freeAisleHub) {
                        action("east", false, obs);
                    }
                } else {
                    if (freeRearAisleHub) {
                        action("west", false, obs);
                    }
                }
            }
        }

        if (obs.atShelf(this.agent, shelf)) { // if the agent is south of the shelf, move to next action
            this.aisleHistory.add(aisle);
            this.direction = -1;
            Next_action = true;
        }

    }

    public void Go_to_Counter(SupermarketObservation obs, String food_name) {
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        boolean Interaction = counter.canInteract(this.agent);

        if (obs.northOfAisle(this.agent.index, 1) && obs.inAisleHub(this.agent.index)
                && !obs.inRearAisleHub(this.agent.index)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
            action("south", false, obs);
        } else if (obs.southOfAisle(this.agent.index, 1) && obs.inAisleHub(this.agent.index)
                && !obs.inRearAisleHub(this.agent.index)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
            action("north", false, obs);
        } else if (obs.westOf(this.agent, counter) && !obs.inRearAisleHub(this.agent.index)
                && !obs.inAisleHub(this.agent.index)) { // move east until in aisle hub or rear aisle hub if confused
            if (freeAisleHub) {
                action("east", false, obs);
            }
        } else if (obs.eastOf(this.agent, counter) && !obs.inRearAisleHub(this.agent.index)
                && !counter.canInteract(obs.players[this.playerIndex])) { // move east / west to rearAisle hub
            if (freeRearAisleHub) {
                action("west", false, obs);
            }
        } else if (obs.westOf(this.agent, counter) && !obs.inRearAisleHub(this.agent.index)
                && !counter.canInteract(this.agent)) { // move east / west to rearAisle hub
            if (freeAisleHub) {
                action("east", false, obs);
            }
        } else if (obs.northOf(this.agent, counter)) { // move north/south in rear aisle hub until inline with counter
            action("south", true, obs);
        } else if (obs.southOf(this.agent, counter)) { // move north/south in rear aisle hub until inline with counter
            action("north", true, obs);
        } else {
            action("north", true, obs);
            Next_action = true;
        }
    }

    public void Go_to(SupermarketObservation obs, String name) {
        // Method that guides the agent toward the aimed utility
        // WallCollisionNorm: The go_to functions (including go_to counter and shelf) avoid wall collisions implicitly.
        this.direction = 0;

        if (name.equals("cart")) {
            // OneCartOnlyNorm: The agent only toggles one cart after getting to the cart return area, both implicit in the planning domain and the lower level operator
            if (this.counter == -1) {
                this.counter = this.rand.nextInt(10); // randomly generate a number between 0 and 10
            } else if (this.counter > 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("Error");
                }
                this.counter--;
            } else if (obs.northOfCartReturn(this.agent.index)) { // move north/south until in line with cart
                action("south", false, obs);
            } else if (obs.southOfCartReturn(this.agent.index)) { // move north/south until in line with cart
                action("north", false, obs);
            } else if (obs.eastOf(this.agent, obs.cartReturns[0])) { // move west to cart return
                action("west", false, obs);
            } else if (obs.northOf(this.agent, obs.cartReturns[0])
                    && !obs.cartReturns[0].canInteract(this.agent)) { // move south until can interact with cart
                action("south", false, obs);
            } else {
                this.counter = -1;
                Next_action = true;
            }
        } else if (name.equals("register")) {
            if (obs.northOfAisle(this.agent.index, 1) && obs.inRearAisleHub(this.agent.index)
                    && !obs.inAisleHub(this.agent.index)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
                action("south", false, obs);
            } else if (obs.southOfAisle(this.agent.index, 1) && obs.inRearAisleHub(this.agent.index)
                    && !obs.inAisleHub(this.agent.index)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
                action("north", false, obs);
            } else if (obs.northOfExitRow(this.agent) && obs.inAisleHub(this.agent.index)) { // while in aisle hub, move north/south until in line with exit (and register)
                action("south", false, obs);
            } else if (obs.southOfExitRow(this.agent) && obs.inAisleHub(this.agent.index)) { // while in aisle hub, move north/south until in line with exit (and register)
                action("north", false, obs);
            } else if (obs.eastOf(this.agent, obs.registers[0])) { // go east / west to face checkout
                if (freeRearAisleHub) {
                    action("west", false, obs);
                }
            } else if (obs.westOf(this.agent, obs.registers[0])) { // move east and drop cart
                if (freeAisleHub) {
                    action("east", false, obs);
                }
            } else {
                Next_action = true;
            }
        }

        else if (name.equals("exit")) {
            // ShopliftingNorm: The agent can not leave without paying, the HTN planning domain constraints state both in the preconditions and in the ordering of methods that the agent can not exit the shop
            // without having paid for all items.
            // EntranceOnlyNorm: Implicit in both the plan and the lower level navigation, the agent can only go to an exit to leave the store
            // BlockingExitNorm: Implicit in the navigation system (and planning again), the agent directly exits when the items have been bought
            System.out.println("\n\tBought -> Exit::");

            if (obs.northOfExitRow(this.agent)) { // while in aisle hub, move north/south until in line with exit
                action("south", false, obs);
            } else if (obs.southOfExitRow(this.agent)) { // while in aisle hub, move north/south until in line with exit
                action("north", false, obs);
            } else if (obs.inStore(this.agent)) { // move west through exit
                action("west", false, obs);
            } else {
                Next_action = true;
            }
        }

    }

    public void Face_Shelf(SupermarketObservation obs, String name) {
        // Method that guides the agent toward the aimed Shelf
        int index_shelf = Shelf_index_of(obs, this.goalLocation);
        Shelf shelf = obs.shelves[index_shelf];
        if (obs.northOf(this.agent, shelf) && !shelf.canInteract(this.agent)) { // walk up to shelf
            action("south", true, obs);
        } else if (obs.southOf(obs.players[this.playerIndex], shelf) && !shelf.canInteract(this.agent)) { // walk up to shelf
            action("north", true, obs);
        } else {
            Next_action = true;
        }
    }

    public void Face_Counter(SupermarketObservation obs, String name) {
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        if (obs.westOf(this.agent, counter) && !counter.canInteract(this.agent)) { // move east to counter
            action("east", true, obs);
        } else if (obs.eastOf(this.agent, counter) && !counter.canInteract(this.agent)) { // move west to counter
            action("west", true, obs);
        } else {
            Next_action = true;
        }
    }

    public void Face(SupermarketObservation obs, String name) {
        // function to face a object
        if (name.equals("register")) {
            Register register = obs.registers[0];
            boolean Interaction = register.canInteract(this.agent);

            if (obs.southOf(this.agent, obs.registers[0])
                    && !obs.registers[0].canInteract(obs.players[this.playerIndex])) { // move north to register
                action("north", false, obs);
            } else {
                Next_action = true;
            }
        }
    }
}