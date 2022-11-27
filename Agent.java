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

import com.supermarket.*;
import com.supermarket.SupermarketObservation.Player;
import com.supermarket.SupermarketObservation.Cart;
import com.supermarket.SupermarketObservation.Shelf;
import com.supermarket.SupermarketObservation.Counter;
import com.supermarket.SupermarketObservation.Basket;
import com.supermarket.SupermarketObservation.Register;




public class Agent extends SupermarketComponentImpl{


    private String goalLocation = "strawberry milk";
    private String partialPlan = "";
    private ArrayList<String> Plan = new ArrayList<>();
    private boolean Flag = false;
    private boolean Next_action = false;
    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
    private String action = "";
    private boolean Husle = false;

    List<String> food = Arrays.asList("milk", "chocolate milk", "strawberry milk", "apples", "oranges", "banana", "strawberry", "raspberry", "sausage", "steak",
    "chicken", "ham", "brie cheese", "swiss cheese", "cheese wheel", "garlic", "leek", "red bell pepper", "carrot", "lettuce", "avocado", "broccoli", "cucumber", "yellow bell pepper");
    List<String> counters = Arrays.asList("fresh fish", "prepared foods");
    List<String> notable_places = Arrays.asList("exit", "register", "cart");

    public Agent() {
        super();
        System.out.println("The goal is: ");
        System.out.println(this.goalLocation);
        shouldRunExecutionLoop = true;
    }

    @Override
    protected void executionLoop() {
        // this is called every 100ms
        // put your code in here, e.g.

        SupermarketObservation obs = getLastObservation();
        Player agent = obs.players[0];

        if (this.partialPlan.length() == 0){this.partialPlan = Get_initial_state(obs);}
        
        if (this.Plan.size() == 0 && !(this.Flag)){Get_plan(obs); this.Flag = true; this.Plan = Read_plan(); Next_action = true;}

        if (this.Plan.size() > 0 && this.Flag){
            
            if (Next_action){
            this.action = this.Plan.remove(0);
            Dictionary action_dict = CollectionDictionary.main(action.replaceAll("\\d","").split(" "));
            System.out.println("\n\tNext action::");
            System.out.println(action_dict);
            Next_action = false;
            }
            //Dictionary action_dict = CollectionDictionary.main(action.split(" "));
            Dictionary action_dict = CollectionDictionary.main(action.replaceAll("\\d","").split(" "));
            if (Next_action){

            }
            //System.out.println(action_dict.get(1));
            if (((String) action_dict.get(1)).contains("navigate")){
                Next_action = false;
                if (((String) action_dict.get(4)).contains("pickup")){
                    this.goalLocation = "cart";
                }
                else if (((String) action_dict.get(4)).contains("counter")){
                    this.goalLocation = "register";
                }
                else{
                this.goalLocation = (String) ((String) action_dict.get(4)).replace("_", " ");;
                }
                //System.out.println(this.goalLocation);


                if (this.food.contains(this.goalLocation)){
                     Go_to_Shelf(obs, agent, this.goalLocation);
                 }
                 else if (this.counters.contains(this.goalLocation)){
                     Go_to_Counter(obs, agent, this.goalLocation);
                 }
                 else if (this.notable_places.contains(this.goalLocation)){
                     Go_to(obs, agent, this.goalLocation);
                 }
            } else if(((String) action_dict.get(1)).contains("release_cart")){
                toggleShoppingCart();
                Next_action = true;
                System.out.println("\n\tRelease::");
            } else if(((String) action_dict.get(1)).contains("grab_cart")){
                toggleShoppingCart();
                Next_action = true;
            } else if(((String) action_dict.get(1)).contains("buy")){
                this.goalLocation = "register";
                Go_to(obs, agent, this.goalLocation);
                interactWithObject();
                interactWithObject();
                Next_action = true;
            } else if(((String) action_dict.get(1)).contains("face_cart")){
                this.goalLocation = (String) ((String) action_dict.get(3)).replace("_", " ");
                //Cart cart1 = obs.carts[agent.curr_cart];
                // int cartIndex = agent.curr_cart;
				Cart cart = obs.carts[0];
                if (agent.direction != cart.direction){  // NORTH is 0, SOUTH is 1, EAST is 2, WEST is 3
                    if (cart.direction == 0){goNorth();}
                    else if (cart.direction == 1){goSouth();}
                    else if (cart.direction == 2){goEast();}
                    else {goWest();}}
                else{Next_action = true;}

                //Next_action = true;
            } else if(((String) action_dict.get(1)).contains("face_object")){
                this.goalLocation = (String) ((String) action_dict.get(3)).replace("_", " ");
                if (((String) action_dict.get(4)).contains("pickup")){
                    this.goalLocation = "cart";
                }
                int index_shelf = Shelf_index_of(obs, this.goalLocation);
                if (this.food.contains(this.goalLocation)){
                    Face_Shelf(obs, agent, this.goalLocation);
                }
                else if (this.counters.contains(this.goalLocation)){
                    Face_Counter(obs, agent, this.goalLocation);
                }
                else if (this.notable_places.contains(this.goalLocation)){
                    Face(obs, agent, this.goalLocation);
                }
                Next_action = true;
            } else if(((String) action_dict.get(1)).contains("get_cart")){
                this.goalLocation = "cart";
                Go_to(obs, agent, this.goalLocation);
                interactWithObject();
                interactWithObject();
                Next_action = true;
            } else if(!(((String) action_dict.get(1)).contains("nop")) || !(action_dict.isEmpty())){
                interactWithObject();
                interactWithObject();
                Next_action = true;
            } else {}}
        }

    public static class CollectionDictionary {
            public static Dictionary main(String[] args) {
               ArrayList<String> list = new ArrayList<String>();
               for (String elem : args)
               {
                list.add(elem);
               }
               Dictionary dictionary = new Hashtable();
               Hashtable<Integer, String> hashTable = new Hashtable<Integer, String>();
               for (int i = 0; i < args.length; i++){
                hashTable.put(i+1, list.get(i));
               }
               return hashTable;
            }
         }

    public String Get_initial_state(com.supermarket.SupermarketObservation obs){
        // Method that returns the partial plan from the observation in a HDDL file
        String[] shopping_list = obs.players[0].shopping_list;
        int[] shopping_quant = obs.players[0].list_quant;
        String object;
        String objects = "";
        String init_state = "";
        String buffer = "";

        int list_counter = 0;

        // for loop
        for (String iterator: shopping_list) {
            object = iterator.replace(" ", "_");
            for(int item_counter = 0; item_counter < shopping_quant[list_counter]; item_counter++){
                object += item_counter;
                objects += object +" ";
                if (buffer != ""){init_state += "(on " + buffer +" "+ object + ") ";}
                else{init_state += "(clear " + object +") ";}
                buffer = object;
                }
            list_counter += 1;
            }
        objects += "- food";
        init_state += "(on " + buffer +" list) ";
        System.out.println(init_state);
        //Instantiating the File class
        String filePath = "shopping.hddl";
        //Instantiating the Scanner class to read the file
        try {
        Path path = Paths.get("shopping.hddl");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        lines.set(7, objects);
        lines.set(19, init_state);
        Files.write(path, lines, StandardCharsets.UTF_8);
        }
        catch (IOException ex) {}
        return init_state;
    }


    public void Get_plan(com.supermarket.SupermarketObservation obs){
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)
        

        String[] shopping_list = obs.players[0].shopping_list;
        System.out.println("\n--------------------------- The shopping list is: ----------------------------\n");
        System.out.println(Arrays.toString(obs.players[0].shopping_list));

        List<String> intermediate_plan = new ArrayList<String>();

        try {
            String[] args = new String[] {"/bin/bash", "-c", "./lilotane domain.hddl shopping.hddl -v=0 | cut -d' ' -f2- | sed 1,2d | head -n -2 > Plan.txt"};
            Process proc = new ProcessBuilder(args).start();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (IOException ex) {}
        System.out.println("Plan saved");
        return;
    }

    public String Get_action(ArrayList<String> Plan){
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)
        String a = "";
        return a;
    }


    public ArrayList<String> Read_plan(){
        // Method that creates the plan from the partial plan (using Lifted Logic for Task Networks: SAT-driven Totally-ordered Hierarchical Task Network (HTN) Planning)
        
        String[] lines = {};
        List<String> grp = new ArrayList<String>();

        try {

            File f = new File("Plan.txt");

            BufferedReader b = new BufferedReader(new FileReader(f));

            String readLine = "";

            System.out.println("\n----------------------------- The Plan is: ------------------------------\n");

            while ((readLine = b.readLine()) != null) {
                if (isNumeric(Arrays.asList(readLine.split(" ", 2)).get(0))){break;}
                System.out.println(readLine);
                grp.add(readLine);
            }
            System.out.println("\n-------------------------------------------------------------------------\n");
            System.out.println("\n---------------------------------Start-----------------------------------\n");

        }
        catch (IOException ex) {}
        return (ArrayList<String>) grp;
    }


    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false; 
        }
        return pattern.matcher(strNum).matches();
    }


    public int Shelf_index_of(SupermarketObservation obs, String food_name){
        // Method that returns the index of the Shelf of the aimed food
        int counter = 0;
        for (Shelf element : obs.shelves){
            //System.out.println(element.food_name);
            if (element.food_name.equals(food_name)){break;}
            else{
                counter = counter + 1;
            }
        }
        return counter;
    }

    public int Counter_index_of(SupermarketObservation obs, String food_name){
        // Method that returns the index of the Counter of the aimed food
        int counter = 0;
        for (Counter element : obs.counters){
            if (element.food.equals(food_name)){break;}
            else{
                counter = counter + 1;
            }
        }
        return counter;
        }

    public void Go_to_Shelf(SupermarketObservation obs, Player agent, String food_name){
        // Method that guides the agent toward the aimed Shelf
        int index_shelf = Shelf_index_of(obs, this.goalLocation);
        Shelf shelf = obs.shelves[index_shelf];
        int aisle = index_shelf / 5 + 1;
        boolean in_aisle = (!(obs.southOfAisle(agent.index, aisle)) && !(obs.northOfAisle(agent.index, aisle)));

        if (in_aisle){              
            if (obs.westOf(agent, shelf)){goEast();}
            else if (obs.eastOf(agent, shelf)){goWest();}
            else if (obs.atShelf(agent, shelf) && !shelf.canInteract(agent)){goNorth();}
            else{}
            }
        else if (obs.inAisleHub(agent.index) || obs.inRearAisleHub(agent.index)){
            if (obs.northOfAisle(agent.index, aisle)){
                goSouth();
            }
            else if (obs.southOfAisle(agent.index, aisle)){
                goNorth();
            }}
        else if (agent.position[0] <= (obs.shelves[0].position[0] - 3)){goEast();}
        else if (agent.position[0] >= (obs.shelves[4].position[0] + 3)){goWest();}
        else{goEast();}

        
        //else if (obs.westOf(agent, obs.shelves[0]) && !(obs.inAisleHub(agent.index))){goEast();}
        //else if (obs.eastOf(agent, obs.shelves[4]) && !(obs.inRearAisleHub(agent.index))){goWest();}
        //else{}
 
        if (obs.atShelf(agent, shelf)){Next_action = true;}
        
    }

    public void Go_to_Counter(SupermarketObservation obs, Player agent, String food_name){
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        boolean Interaction = counter.canInteract(agent);
        int aisle = 3;
        boolean in_aisle = (!(obs.southOfAisle(agent.index, aisle)) && !(obs.northOfAisle(agent.index, aisle)));
        boolean in_counter = (!(obs.southOf(agent, counter)) && !(obs.northOf(agent, counter)));

        if (in_counter){
            if (obs.westOf(agent, counter)){goEast();}
            else if (obs.eastOf(agent, counter)){goWest();}
            else if (!counter.canInteract(agent)){goNorth();
            }  }
        else if (in_aisle){              
            if (obs.westOf(agent, counter)){goEast();}
            else if (obs.eastOf(agent, counter)){goWest();}
            }
        else if (obs.inRearAisleHub(agent.index)){
            if (obs.northOfAisle(agent.index, aisle)){
                goSouth();
            }
            else if (obs.southOfAisle(agent.index, aisle)){
                goNorth();
            }}
        else if (obs.inAisleHub(agent.index)){
            if (obs.northOf(agent, counter)){
                goSouth();
            }
            else if (obs.southOf(agent, counter)){
                goNorth();
            }}
        //else if (agent.position[0] <= (obs.shelves[0].position[0] - 3)){goEast();}
        //else if (agent.position[0] >= (obs.shelves[4].position[0] + 3)){goWest();}
        else{goWest();}
        if (Interaction){Next_action = true;}
    }

    public void Go_to(SupermarketObservation obs, Player agent, String name){
        // Method that guides the agent toward the aimed utility
        if (name.equals("cart")){
            boolean Interaction = !obs.southOfCartReturn(agent.index) && !obs.northOfCartReturn(agent.index);

            if (!(obs.atCartReturn(agent.index))){
                goSouth();
            }
            else {Next_action = true;}

            }
        else if (name.equals("register")){
            Register register = obs.registers[0];
            boolean Interaction = register.canInteract(agent);
            int aisle = 3;
            boolean in_aisle = (!(obs.southOfAisle(agent.index, aisle)) && !(obs.northOfAisle(agent.index, aisle)));
            boolean in_register = (agent.position[1] < register.position[1]-4 && agent.position[1] > register.position[1]-6);

            if (in_register){
                if (agent.position[0] <= (register.position[0] + 3)){goEast();}
                else if (obs.eastOf(agent, register)){goWest();}
                else if (!register.canInteract(agent)){goNorth();
                }  }
            else if (in_aisle){              
                if (obs.westOf(agent, register)){goEast();}
                else if (obs.eastOf(agent, register)){goWest();}
                }
            else if (obs.inRearAisleHub(agent.index)){
                if (obs.northOfAisle(agent.index, aisle)){
                    goSouth();
                }
                else if (obs.southOfAisle(agent.index, aisle)){
                    goNorth();
                }}
            else if (obs.inAisleHub(agent.index)){
                if (obs.northOf(agent, register)){
                    goSouth();
                }
                else if (obs.southOf(agent, register)){
                    goNorth();
                }}
            //else if (agent.position[0] <= (obs.shelves[0].position[0] - 3)){goEast();}
            //else if (agent.position[0] >= (obs.shelves[4].position[0] + 3)){goWest();}
            else{goWest();}
            if (Interaction){Next_action = true;}
        }

        else if (name.equals("exit")){
            boolean Interaction = !obs.southOfExitRow(agent) && !obs.northOfExitRow(agent);

            if (obs.westOf(agent, obs.shelves[0]) && !(obs.inAisleHub(agent.index)) && !Interaction){goEast();}
            else if (obs.eastOf(agent, obs.shelves[0]) && !(obs.inAisleHub(agent.index))&& !Interaction){goWest();}
            else if (obs.southOfExitRow(agent)){goNorth();}
            else if (obs.northOfExitRow(agent)){goSouth();}
            else {goWest();}
            if (Interaction){Next_action = true;}
        } 
        
    }

    public void Face_Shelf(SupermarketObservation obs, Player agent, String name){
        // Method that guides the agent toward the aimed Shelf
        goNorth();
    }

    public void Face_Counter(SupermarketObservation obs, Player agent, String name){
        // Method that guides the agent toward the aimed Counter
        Counter counter = obs.counters[Counter_index_of(obs, this.goalLocation)];
        boolean Interaction = counter.canInteract(agent);
    }

    public void Face(SupermarketObservation obs, Player agent, String name){

        if (name.equals("register")){
            Register register = obs.registers[0];
            boolean Interaction = register.canInteract(agent);

            if (obs.westOf(agent, register)){goEast();}
            else if (obs.eastOf(agent, register)){goWest();}
            else if (obs.southOf(agent, register)){goNorth();}
            else if (obs.northOf(agent, register)){goSouth();}
            else if (!Interaction){goWest();}
        }
    }

}