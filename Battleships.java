package battleships;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Battleships {
	/** Random number generator for our auto play game strategy. */
    static Random r = new Random();

    private static float[][] priorities = null;
    // Priority is a float in [0,1]
    private static int boardWidth;
    private static int boardHeight;
    private static int[][] placement = null;

    /* Use this method to define the bot's strategy.
     */
    public static BattleshipsMove calculateMove(MainWindow.GameState state) {
        if(state.Round==0) {
            boardHeight = state.MyBoard.size();
            boardWidth = state.MyBoard.get(0).size();
            priorities = new float[boardHeight][boardWidth];
            placement = new int[boardHeight][boardWidth];
            for(int i = 0; i < boardHeight; ++i){
                for(int j = 0; j < boardWidth; ++j){
                    priorities[i][j] = 1;
                    if (state.MyBoard.get(i).get(j).equals("")){
                        placement[i][j] = 100;
                    }
                }
            }
        	return placeShips(state);
        }
        else {
        	return findShips(state);
        }
    }

    private static ArrayList<Integer> findMax() {
        ArrayList<Integer> maximum = new ArrayList<>();
        int maxVal = 0;
        for (int i = 0; i < placement.length; i++) {
            for (int j = 0; j < placement[0].length; j++) {
                if (maxVal < placement[i][j]) {
                    maxVal = placement[i][j];
                    maximum = new ArrayList<>();
                    maximum.add(i * placement[0].length + j);
                } else if (maxVal == placement[i][j]) {
                    maximum.add(i * placement[0].length + j);
                }
            }
        }
        return maximum;
    }

    private static ArrayList<ShipPosition> findValidPosition (int length, ArrayList<ArrayList<String>> board) {
        ArrayList<ShipPosition> result = new ArrayList<>();
        for (int i = 0; i < board.size(); i++) {
            for (int j = 0; j < board.get(0).size(); j++) {
                if(i + length - 1 >= board.size()) { //If ship doesn't fit within board boundaries VERTICALLY
                    continue;
                }
                for(int l=0; l < length; l++) {
                    String square = board.get(i+l).get(j);
                    if(!square.equals("")) {
                        continue;
                    }
                }
                result.add(new ShipPosition(Character.toString((char)(i+65)),j,"V"));
            }
        }
        for (int i = 0; i < board.size(); i++) {
            for (int j = 0; j <board.get(0).size(); j++) {
                if(j + length - 1 >= board.get(0).size()) { //If ship doesn't fit within board boundaries
                    continue;
                }
                for(int l=0; l < length; l++) {
                    String square = board.get(i).get(j+l);
                    if(!square.equals("")) {
                        continue;
                    }
                }
                result.add(new ShipPosition(Character.toString((char) (i + 65)), j, "H"));
            }
        }
        return result;
    }

    private static ShipPosition findBestPosition(ArrayList<ShipPosition> validPositions, int length) {
        int max = 0;
        ShipPosition result = null;
        for (int i = 0; i < validPositions.size(); i++) {
            int sum = 0;
            if (validPositions.get(i).Orientation.equals("V")) {
                for (int j = 0; j < length; j++) {
                    sum += placement[validPositions.get(i).Row.toCharArray()[0] -65 + j][validPositions.get(i).Column];
                }
                if (max < sum) {
                    max = sum;
                    result = validPositions.get(i);
                }
            }
        }
        return result;
    }

    private static BattleshipsMove placeShips(MainWindow.GameState state) {
    	ArrayList<ShipPosition> placement = new ArrayList<ShipPosition>();
    	int row = 0, column = 0;
    	String orientation = "";
	    for (int i = 0; i < state.Ships.size(); i++) {
	    	boolean placed = false;
	    	while(!placed) {
	    		ArrayList<ArrayList<String>> board = state.MyBoard;
	    		ShipPosition newPlace = findBestPosition(findValidPosition(state.Ships.get(i),state.MyBoard), state.Ships.get(i));
	    		row = newPlace.Row.toCharArray()[0] - 65;
	    		column = newPlace.Column;
	    		orientation = newPlace.Orientation;
	    		/*
	    		Put code on how to modify placement[][] priority here, based on the new ship to be placed.
	    		 */
	    		board = attemptShipPlacement(row, column, state.MyBoard, state.Ships.get(i), orientation, i);
	    		if(board != null) {
	    			placed = true;
	    			state.MyBoard = board;
	    		}
	    	}
	    	placement.add(new ShipPosition(Character.toString((char)(row+65)), column+1, orientation));
	    }
	    return new BattleshipsMove(placement, "", 0);
    }
    
    private static BattleshipsMove findShips(MainWindow.GameState state) {
    	boolean placed = false;
		int row=0, column=0;
    	while(!placed) {
    		ArrayList<ArrayList<String>> board = state.MyBoard;
    		row = r.nextInt(board.size());
    		column = r.nextInt(board.get(0).size());
    		if(state.OppBoard.get(row).get(column).equals("") || state.OppBoard.get(row).get(column).equals("L")) {
    			placed = true;
    		}
    	}
    	return new BattleshipsMove(null, Character.toString((char)(row+65)), column+1);
    }
    
    private static ArrayList<ArrayList<String>> attemptShipPlacement(int i, int j, ArrayList<ArrayList<String>> board, int length, String orientation, int shipNum){
    	if(orientation.equals("V")) { //Vertical
    		if(i + length - 1 >= board.size()) { //If ship doesn't fit within board boundaries
    			return null;
    		}
    		for(int l=0; l < length; l++) {
    			String square = board.get(i+l).get(j);
    			if(!square.equals("")) {
    				return null;
    			}
    		}
    		for(int l=0; l < length; l++) {
    			board.get(i+l).set(j, Integer.toString(shipNum));
    		}
    		return board;
    	} else { //Horizontal
    		if(j + length - 1 >= board.get(0).size()) { //If ship doesn't fit within board boundaries
    			return null;
    		}
    		for(int l=0; l < length; l++) {
    			String square = board.get(i).get(j+l);
    			if(!square.equals("")) {
    				return null;
    			}
    		}
    		for(int l=0; l < length; l++) {
    			board.get(i).set(j+l, Integer.toString(shipNum));
    		}
    		return board;
    	}
    }
}