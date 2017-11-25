package battleships;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.abs;

public class Battleships {
	/** Random number generator for our auto play game strategy. */
    static Random r = new Random();

    private static float[][] priorities = null;
    // Priority is a float in [0,1]
    private static int boardWidth;
    private static int boardHeight;
    private static class Shot {int row; int col; Shot(int a, int b){row = a; col = b;}}
    private static Shot lastShot = null;
    private static boolean trackingShip = false;
    private static int direction = 0;
    private static int Vertical = 1;
    private static int Horiztonal = 2;
    private static int[][] localShape = {{-1,0},{0,-1},{0,1},{1,0}};
    private static Shot trackOrigin;
    private static int[][] transitions = {{1,1},{3,1},{6,3},{7,4},{8,5},{9,1},{6,3},{7,1},{8,5},{9,1}};
    private static int stateCount = 10;
    private static int currentState = 0;
    private static String shotResult;
    /* Use this method to define the bot's strategy.
     */
    public static BattleshipsMove calculateMove(MainWindow.GameState state) {
        if(state.Round==0) {
            boardHeight = state.MyBoard.size();
            boardWidth = state.MyBoard.get(0).size();
            priorities = new float[boardHeight][boardWidth];
            for(int i = 0; i < boardHeight; ++i){
                for(int j = 0; j < boardWidth; ++j){
                    if (state.MyBoard.get(i).get(j).equals(""))
                        priorities[i][j] = abs(i - j) % 2 == 0 ? 0.5f : 0.3f;
                    else
                        priorities[i][j] = 0;
                }
            }
            trackingShip = false;
            currentState = 0;
            shotResult = "";
        	return placeShips(state);
        }
        else {

        	return findShipWithStateMachine(state);
        }
    }
    
    private static BattleshipsMove placeShips(MainWindow.GameState state) {
    	ArrayList<ShipPosition> placement = new ArrayList<ShipPosition>();
    	int row = 0, column = 0;
    	String orientation = "";
	    for (int i = 0; i < state.Ships.size(); i++) {
	    	boolean placed = false;
	    	while(!placed) {
	    		ArrayList<ArrayList<String>> board = state.MyBoard;
	    		row = r.nextInt(board.size());
	    		column = r.nextInt(board.get(0).size());
	    		String[] orientations = {"H","V"};
	    		orientation = orientations[r.nextInt(2)];
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

    private static void firstHit(int row, int col) {
        for (int i = 0; i < 4; ++i) {
            int newRow = row + localShape[i][0];
            int newCol = col + localShape[i][1];
            if (newRow >= 0 && newRow < boardHeight && newCol >= 0 && col + newCol < boardWidth) {
                if (priorities[newRow][newCol] != 0) {
                    priorities[newRow][newCol] += 0.2;
                }
            }
        }
    }

    private static Shot randomShot() {
        int maxCol = 0;
        int maxRow = 0;
        float maxP = 0;
        for(int i = 0; i < boardHeight; ++i){
            for(int j = 0; j <boardWidth; ++j){
                if (priorities[i][j] > maxP) {
                    maxRow = i;
                    maxCol = j;
                    maxP = priorities[i][j];
                }
            }
        }
        return new Shot(maxRow, maxCol);
    }

    private static BattleshipsMove executeS() {
        switch (currentState){
            case 1:
                lastShot = randomShot();
                break;
            case 2:
                lastShot = new Shot(lastShot.row - 1, lastShot.col);
                break;
            case 3:
                lastShot = new Shot(lastShot.row + 1, lastShot.col);
                break;
            case 4:
                lastShot = new Shot(lastShot.row, lastShot.col - 1);
                break;
            case 5:
                lastShot = new Shot(lastShot.row, lastShot.col + 1);
                break;
            case 6:
                lastShot = new Shot(lastShot.row - 1, lastShot.col);
                break;
            case 7:
                lastShot = new Shot(lastShot.row + 1, lastShot.col);
                break;
            case 8:
                lastShot = new Shot(lastShot.row, lastShot.col - 1);
                break;
            case 9:
                lastShot = new Shot(lastShot.row, lastShot.col + 1);
                break;
        }
        System.out.print("Fire at ");
        System.out.print(lastShot.row);
        System.out.println(lastShot.col);
        priorities[lastShot.row][lastShot.col] = 0;
        return new BattleshipsMove(null, Character.toString((char)(lastShot.row + 65)), lastShot.col + 1);
    }

    private static boolean canShoot(int row, int col){
        if (row >= 0 && row < boardHeight && col >= 0 && col < boardWidth) {
            return priorities[row][col] != 0;
        } else {
            return false;
        }
    }

    private static boolean available(int state) {
        switch (state){
            case 1:
                return true;
            case 2:
                return canShoot(lastShot.row - 1, lastShot.col);
            case 3:
                return canShoot(lastShot.row + 1, lastShot.col);
            case 4:
                return canShoot(lastShot.row, lastShot.col - 1);
            case 5:
                return canShoot(lastShot.row, lastShot.col + 1);
            case 6:
                return canShoot(lastShot.row - 1, lastShot.col);
            case 7:
                return canShoot(lastShot.row + 1, lastShot.col);
            case 8:
                return canShoot(lastShot.row, lastShot.col - 1);
            case 9:
                return canShoot(lastShot.row, lastShot.col + 1);
            default:
                return true;
        }
    }

    private static BattleshipsMove findShipWithStateMachine(MainWindow.GameState state) {
        if (currentState == 0){
            currentState = 1;
            return executeS();
        } else {
            shotResult = state.OppBoard.get(lastShot.row).get(lastShot.col);
            if (shotResult.equals("H")) {
                currentState = transitions[currentState][0];
            } else if (shotResult.equals("M")) {
                currentState = transitions[currentState][1];
            }
            while (!available(currentState)) {
                currentState = transitions[currentState][1];
                System.out.println(currentState);
            }
            return executeS();
        }
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