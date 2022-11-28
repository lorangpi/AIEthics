
/*
Pierrick Lorang & Brennan Miller-Klugman
11/27/22
Shopping Agent #2

Referances:
    - Java API https://docs.oracle.com/javase/7/docs/api/
    - W3 schools https://www.w3schools.com/java/ (to brush up on how to use Array Lists and random generators)
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.Random;

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
    private boolean hasCart = false; // boolean to keep track of if cart is currently in posession of player
    private ArrayList<String> sequence = new ArrayList<String>(); // used to keep track of a sequence of actions, this can be reversed after an objective is completed to return to aisleHub
    private Random rand = new Random(); // random generator
    private int direction = -1;
    private int counter = -1; // counter used to track of the number of times the player has moved south, defaults to -1
    private List<Integer> aisleHistory = new ArrayList<Integer>(); // keeps track of previously entered aisles, used to determine if the player should move south

    List<String> food = Arrays.asList("milk", "chocolate milk", "strawberry milk", "apples", "oranges", "banana",
            "strawberry", "raspberry", "sausage", "steak",
            "chicken", "ham", "brie cheese", "swiss cheese", "cheese wheel", "garlic", "leek", "red bell pepper",
            "carrot", "lettuce", "avocado", "broccoli", "cucumber", "yellow bell pepper", "onion");
    List<String> counters = Arrays.asList("fresh fish", "prepared foods");
    List<String> notable_places = Arrays.asList("exit", "register", "cart");

    public Agent() {
        super();
        shouldRunExecutionLoop = true;

    }

    @Override
    protected void executionLoop() {
        // this is called every 100ms
        // put your code in here, e.g.

        this.rand.setSeed(System.nanoTime()); // randomize seed for random number generator

        SupermarketObservation obs = getLastObservation();
        Player agent = obs.players[0];

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
                    Go_to_Shelf(obs, agent, this.goalLocation);
                } else if (this.counters.contains(this.goalLocation)) {
                    Go_to_Counter(obs, agent, this.goalLocation);
                } else if (this.notable_places.contains(this.goalLocation)) {
                    Go_to(obs, agent, this.goalLocation);
                }
            } else if (((String) action_dict.get(1)).contains("release_cart")) {
                action("toggle", true, obs);
                Next_action = true;
                System.out.println("\n\tRelease::");
            } else if (((String) action_dict.get(1)).contains("grab_cart")) {
                int size = this.aisleHistory.size();
                if (this.counter == -1) {
                    this.counter = this.rand.nextInt(3) + 4; // random number between 4 and 6
                } else if (this.counter != 0
                        && ((size < 2) || (this.aisleHistory.get(size - 1) != this.aisleHistory.get(size - 2)))) { // move south before exiting an aisle, this should help to prevent collisions between an agent entering and an agent leaving an aisle
                    action("south", false, obs);
                    this.counter -= 1;
                } else {
                    hasCart = !hasCart;
                    this.counter = -1;
                    Next_action = true;
                }

            } else if (((String) action_dict.get(1)).contains("buy")) {
                this.goalLocation = "register";
                Go_to(obs, agent, this.goalLocation);
                interactWithObject();
                //interactWithObject(); //erase comment to display final bought list in the shopping environment
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
                    Face_Shelf(obs, agent, this.goalLocation);
                } else if (this.counters.contains(this.goalLocation)) {
                    Face_Counter(obs, agent, this.goalLocation);
                } else if (this.notable_places.contains(this.goalLocation)) {
                    Face(obs, agent, this.goalLocation);
                } else {
                    Next_action = true;
                }
            } else if (((String) action_dict.get(1)).contains("get_cart")) {
                this.goalLocation = "cart";
                Go_to(obs, agent, this.goalLocation);
                interactWithObject();
                interactWithObject();
                Next_action = true;
            } else if (!(((String) action_dict.get(1)).contains("nop")) || !(action_dict.isEmpty())) {
                interactWithObject();
                interactWithObject();
                Next_action = true;
            } else {
            }
        }
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
        String[] shopping_list = obs.players[0].shopping_list;
        int[] shopping_quant = obs.players[0].list_quant;
        String object;
        String objects = "";
        String init_state = "";
        String buffer = "";

        int list_counter = 0;

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
        String filePath = "shopping.hddl";
        // Instantiating the Scanner class to read the file
        try {
            Path path = Paths.get("shopping.hddl");
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

        String[] shopping_list = obs.players[0].shopping_list;
        System.out.println("\n--------------------------- The shopping list is: ----------------------------\n");
        System.out.println(Arrays.toString(obs.players[0].shopping_list));
        System.out.println(Arrays.toString(obs.players[0].list_quant));

        List<String> intermediate_plan = new ArrayList<String>();

        try {
            String[] args = new String[] { "/bin/bash", "-c",
                    "./lilotane domain.hddl shopping.hddl -v=0 | cut -d' ' -f2- | sed 1,2d | head -n -2 > Plan.txt" };
            Process proc = new ProcessBuilder(args).start();
            Thread.sleep(500);
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

            File f = new File("Plan.txt");

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
            hasCart = !hasCart; // flip hasCart

            this.sequence.add("toggle"); // pickup cart
            this.sequence.add("interact"); // put any items that were picked up inside of the cart

            int dir = obs.players[0].direction; // get direction of cart and append to sequence
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
         * performs the inverse of every action taken since leaving the aisle hub
         * at the end of this sequence, the player will be in the aisle hub again
         */
        int size = this.sequence.size();
        if (size > 0) { // repeat previous steps
            if (!this.sequence.get(size - 1).contains("toggle")) { // check if the next action is to toggle cart, if not proceed
                action(sequence.get(size - 1), false, obs);
                this.sequence.remove(size - 1);
            } else { // sequence is complete, pickup cart
                action("toggle", false, obs);
                this.sequence.clear(); // clear arraylist
                Next_action = true;
            }
        } else {
            System.out.println("Error: sequence was empty");
        }
    }

    public void Go_to_Shelf(SupermarketObservation obs, Player agent, String food_name) {
        // Method that guides the agent toward the aimed Shelf

        // WrongShelfNorm: The agent won't put food at a wrong shelf as it follows a strict plan and can't interact with a shelf if it is holding an item in the domain

        int index_shelf = Shelf_index_of(obs, this.goalLocation);
        Shelf shelf = obs.shelves[index_shelf];
        int aisle = index_shelf / 5 + 1;
        boolean in_aisle = (!(obs.southOfAisle(agent.index, aisle)) && !(obs.northOfAisle(agent.index, aisle)));

        if (this.direction == -1) { // randomly decide to prioritize east or west movement
            this.direction = this.rand.nextInt(2);
        }

        if (in_aisle) { // if in aisle move east / west to shelf
            if (obs.westOf(agent, shelf)) {
                action("east", true, obs);
            } else if (obs.eastOf(agent, shelf)) {
                action("west", true, obs);
            }

        } else if (obs.inAisleHub(agent.index) || obs.inRearAisleHub(agent.index)) { // if in aisle hub or rear aisle hub, move north or south to desired item
            if (obs.northOfAisle(agent.index, aisle)) {
                action("south", false, obs);
            } else if (obs.southOfAisle(agent.index, aisle)) {
                action("north", false, obs);
            }
        } else {
            if (this.direction == 0) { // randomly decide to go east or west if player is already in aisle, this should help to lower the chance of agent collisions
                if (agent.position[0] <= (obs.shelves[0].position[0] - 3)) { // prioritize east
                    action("east", false, obs);
                } else if (agent.position[0] >= (obs.shelves[4].position[0] + 3)) { // move west if must
                    action("west", false, obs);
                } else {
                    action("east", false, obs);
                }
            } else {
                if (agent.position[0] >= (obs.shelves[4].position[0] + 3)) { // prioritize west
                    action("west", false, obs);
                } else if (agent.position[0] <= (obs.shelves[0].position[0] - 3)) { // move east if must
                    action("east", false, obs);
                } else {
                    action("west", false, obs);
                }
            }
        }

        if (obs.atShelf(agent, shelf)) { // if the agent is south of the shelf, move to next action
            this.aisleHistory.add(aisle);
            this.direction = -1;
            Next_action = true;
        }

    }

    public void Go_to_Counter(SupermarketObservation obs, Player agent, String food_name) {
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        boolean Interaction = counter.canInteract(agent);

        if (obs.northOfAisle(0, 1) && obs.inAisleHub(0) && !obs.inRearAisleHub(0)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
            action("south", false, obs);
        } else if (obs.southOfAisle(0, 1) && obs.inAisleHub(0) && !obs.inRearAisleHub(0)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
            action("north", false, obs);
        } else if (obs.westOf(obs.players[0], counter) && !obs.inRearAisleHub(0) && !obs.inAisleHub(0)) { // move east until in aisle hub or rear aisle hub if confused
            action("east", false, obs);
        } else if (obs.eastOf(obs.players[0], counter) && !obs.inRearAisleHub(0)
                && !counter.canInteract(obs.players[0])) { // move east / west to rearAisle hub
            action("west", false, obs);
        } else if (obs.westOf(obs.players[0], counter) && !obs.inRearAisleHub(0)
                && !counter.canInteract(obs.players[0])) { // move east / west to rearAisle hub
            action("east", false, obs);
        } else if (obs.northOf(obs.players[0], counter)) { // move north/south in rear aisle hub until inline with counter
            action("south", true, obs);
        } else if (obs.southOf(obs.players[0], counter)) { // move north/south in rear aisle hub until inline with counter
            action("north", true, obs);
        } else {
            action("north", true, obs);
            Next_action = true;
        }
    }

    public void Go_to(SupermarketObservation obs, Player agent, String name) {
        // Method that guides the agent toward the aimed utility
        this.direction = 0;
        if (name.equals("cart")) {
            // OneCartOnlyNorm: The agent only toggles one cart after getting to the cart return area, both implicit in the planning domain and the lower level operator
            if (obs.northOfCartReturn(0)) { // move north/south until in line with cart
                action("south", false, obs);
            } else if (obs.southOfCartReturn(0)) { // move north/south until in line with cart
                action("north", false, obs);
            } else if (obs.eastOf(obs.players[0], obs.cartReturns[0])) { // move west to cart return
                action("west", false, obs);
            } else if (obs.northOf(obs.players[0], obs.cartReturns[0])
                    && !obs.cartReturns[0].canInteract(obs.players[0])) { // move south until can interact with cart
                action("south", false, obs);
            } else {
                Next_action = true;
            }
        } else if (name.equals("register")) {
            if (obs.northOfAisle(0, 1) && obs.inRearAisleHub(0) && !obs.inAisleHub(0)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
                action("south", false, obs);
            } else if (obs.southOfAisle(0, 1) && obs.inRearAisleHub(0) && !obs.inAisleHub(0)) { // navigate north / west to be in line with a randomly selected aisle (used to traverse to back of store)
                action("north", false, obs);
            } else if (obs.northOfExitRow(obs.players[0]) && obs.inAisleHub(0)) { // while in aisle hub, move north/south until in line with exit (and register)
                action("south", false, obs);
            } else if (obs.southOfExitRow(obs.players[0]) && obs.inAisleHub(0)) { // while in aisle hub, move north/south until in line with exit (and register)
                action("north", false, obs);
            } else if (obs.eastOf(obs.players[0], obs.registers[0])) { // go east / west to face checkout
                action("west", false, obs);
            } else if (obs.westOf(obs.players[0], obs.registers[0])) { // move east and drop cart
                action("east", true, obs);
            } else {
                Next_action = true;
            }
        }

        else if (name.equals("exit")) {
            // ShopliftingNorm: The agent can not leave without paying, the HTN planning domain constraints state both in the preconditions and in the ordering of methods that the agent can not exit the shop without having paid for all items.
            // EntranceOnlyNorm: Implicit in both the plan and the lower level navigation, the agent can only go to an exit to leave the store
            // BlockingExitNorm: Implicit in the navigation system (and planning again), the agent directly exits when the items have been bought
            System.out.println("\n\tBought -> Exit::");
            
            if (obs.northOfExitRow(obs.players[0])) { // while in aisle hub, move north/south until in line with exit
                action("south", false, obs);
            } else if (obs.southOfExitRow(obs.players[0])) { // while in aisle hub, move north/south until in line with exit
                action("north", false, obs);
            } else if (obs.inStore(obs.players[0])) { // move west through exit
                action("west", false, obs);
            } else {
                Next_action = true;
            }
        }

    }

    public void Face_Shelf(SupermarketObservation obs, Player agent, String name) {
        // Method that guides the agent toward the aimed Shelf
        int index_shelf = Shelf_index_of(obs, this.goalLocation);
        Shelf shelf = obs.shelves[index_shelf];
        if (obs.northOf(obs.players[0], shelf) && !shelf.canInteract(obs.players[0])) { // walk up to shelf
            action("south", true, obs);
        } else if (obs.southOf(obs.players[0], shelf) && !shelf.canInteract(obs.players[0])) { // walk up to shelf
            action("north", true, obs);
        } else {
            Next_action = true;
        }
    }

    public void Face_Counter(SupermarketObservation obs, Player agent, String name) {
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        if (obs.westOf(obs.players[0], counter) && !counter.canInteract(obs.players[0])) { // move east to counter
            action("east", true, obs);
        } else if (obs.eastOf(obs.players[0], counter) && !counter.canInteract(obs.players[0])) { // move west to counter
            action("west", true, obs);
        } else {
            Next_action = true;
        }
    }

    public void Face(SupermarketObservation obs, Player agent, String name) {
        // function to face a object
        if (name.equals("register")) {
            Register register = obs.registers[0];
            boolean Interaction = register.canInteract(agent);

            if (obs.southOf(obs.players[0], obs.registers[0]) && !obs.registers[0].canInteract(obs.players[0])) { // move north to register
                action("north", false, obs);
            } else {
                Next_action = true;
            }
        }
    }
}