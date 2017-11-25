package battleships;

import java.util.ArrayList;
import java.util.Random;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.max;

public class Battleships {
    /**
     * Random number generator for our auto play game strategy.
     */
    static Random r = new Random();

    private static float[][] priorities = null;
    // Priority is a float in [0,1]
    private static int boardWidth;
    private static int boardHeight;
    private static int[][] placement = null;
    private static Shot lastShot = null;
    private static int[][] localShape = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static Shot trackOrigin;
    private static int[][] transitions = {{1, 1}, {2, 1}, {6, 3}, {7, 4}, {8, 5}, {9, 1}, {6, 3}, {7, 1}, {8, 5}, {9, 1}};
    private static int stateCount = 10;
    private static int currentState = 0;
    private static String shotResult;
    private static ArrayList<Integer> shipLengths;
    private static int[][] startingPosCount;

    /* Use this method to define the bot's strategy.
     */
    public static BattleshipsMove calculateMove(MainWindow.GameState state) {
        if (state.Round == 0) {
            boardHeight = state.MyBoard.size();
            boardWidth = state.MyBoard.get(0).size();
            startingPosCount = new int[boardHeight][boardWidth];
            priorities = new float[boardHeight][boardWidth];
            placement = new int[boardHeight][boardWidth];
            for (int i = 0; i < boardHeight; ++i) {
                for (int j = 0; j < boardWidth; ++j) {
                    if (state.MyBoard.get(i).get(j).equals("")) {
                        if (i == 0 || i == boardHeight-1 || j == 0 || j == boardWidth-1)
                            placement[i][j] =90;
                        else
                            placement[i][j] = 100;
                    }
                }
            }
            currentState = 0;
            shotResult = "";
            shipLengths = state.Ships;
            return placeShips(state);
        } else {
            return findShipWithStateMachine(state);
        }
    }

    private static ArrayList<ShipPositionToBe> findValidPosition(int length, ArrayList<ArrayList<String>> board) {
        ArrayList<ShipPositionToBe> result = new ArrayList<>();
        for (int i = 0; i < boardHeight - length + 1; i++) {
            for (int j = 0; j < boardWidth; j++) {
                boolean able = true;
                int score = 0;
                for (int l = 0; l < length; l++) {
                    String square = board.get(i + l).get(j);
                    if (!square.equals("")) {
                        able = false;
                        break;
                    }
                    score += placement[i + l][j];
                }
                if (able) {
                    result.add(new ShipPositionToBe(Character.toString((char) (i + 65)), j, "V", score));
                    for (int l = 0; l < length; l++) {
                        startingPosCount[i + l][j]++;
                    }
                }
            }
        }
        for (int i = 0; i < boardHeight; i++) {
            for (int j = 0; j < boardWidth - length + 1; j++) {
                boolean able = true;
                int score = 0;
                for (int l = 0; l < length; l++) {
                    String square = board.get(i).get(j + l);
                    if (!square.equals("")) {
                        able = false;
                        break;
                    }
                    score += placement[i][j + l];
                }
                if (able) {
                    result.add(new ShipPositionToBe(Character.toString((char) (i + 65)), j, "H", score));
                    for (int l = 0; l < length; l++) {
                        startingPosCount[i][j + l]++;
                    }
                }
            }
        }
        return result;
    }

    private static ShipPosition findBestPosition(ArrayList<ShipPositionToBe> validPositions) {
        int max = 0;
        ShipPosition result = null;
        Collections.sort(validPositions,new ByScore());
        for (ShipPositionToBe i : validPositions) {
            System.out.print(i.score);
            System.out.print(' ');
        }
        System.out.println();
        int length = validPositions.size();
        if (length == 1 || validPositions.get(length - 1).score > validPositions.get(length - 2).score){
            return validPositions.get(length - 1);
        }
        int k = 1;
        for(int i = validPositions.size() - 2; i >=0; --i) {
            if (validPositions.get(i).score == validPositions.get(i + 1).score){
                k++;
            } else  {
                break;
            }
        }
        int lucky = r.nextInt(k);
        return validPositions.get(length - 1 - lucky);
    }

    private static BattleshipsMove placeShips(MainWindow.GameState state) {
        int adPenalty = 20;
        ArrayList<ShipPosition> placements = new ArrayList<ShipPosition>();
        for (int i = 0; i < state.Ships.size(); i++) {
            ShipPosition newPlace = findBestPosition(findValidPosition(state.Ships.get(i), state.MyBoard));
            ArrayList<ArrayList<String>> board;
            int row = newPlace.Row.charAt(0) - 'A';
            int col = newPlace.Column;
            String orient = newPlace.Orientation;
            int length = state.Ships.get(i);
            board = attemptShipPlacement(row, col, state.MyBoard, length, orient, i);
            if (orient.equals("V")) {
                for (int n = row - 1; n <= row + length; n++) {
                    if (n < 0) continue;
                    if (n >= state.MyBoard.size()) break;
                    for (int m = col - 1; m <= col + 1; m++) {
                        if (m < 0) continue;
                        if (m >= state.MyBoard.get(0).size()) break;
                        placement[n][m] -= adPenalty;
                    }
                }
            } else {
                for (int n = row - 1; n <= row + 1; n++) {
                    if (n < 0) continue;
                    if (n >= state.MyBoard.size()) break;
                    for (int m = col - 1; m <= col + length; m++) {
                        if (m < 0) continue;
                        if (m >= state.MyBoard.get(0).size()) break;
                        placement[n][m] -= adPenalty;
                    }
                }
            }
            printMat(placement);
            state.MyBoard = board;
            newPlace.Column++;
            placements.add(newPlace);
        }
        int maxP = 0;
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (startingPosCount[i][j] > maxP) maxP = startingPosCount[i][j];
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (state.MyBoard.get(i).get(j).equals(""))
                    priorities[i][j] = startingPosCount[i][j] / ((float)maxP);
                else
                    priorities[i][j] = 0;
            }
        }
        return new BattleshipsMove(placements, "", 0);
    }

    private static BattleshipsMove findShips(MainWindow.GameState state) {
        boolean placed = false;
        int row = 0, column = 0;
        while (!placed) {
            ArrayList<ArrayList<String>> board = state.MyBoard;
            row = r.nextInt(board.size());
            column = r.nextInt(board.get(0).size());
            if (state.OppBoard.get(row).get(column).equals("") || state.OppBoard.get(row).get(column).equals("L")) {
                placed = true;
            }
        }
        return new BattleshipsMove(null, Character.toString((char) (row + 65)), column + 1);
    }

    private static void reducePossiblities(ArrayList<ArrayList<String>> state) {
        // If a cell cannot contain ship, set its priority to 0.
        int shortest = shortestShipLength(state);
        boolean possible[][] = new boolean[boardHeight][boardWidth];
        for (int i = 0; i < boardHeight; ++i) {
            for(int j = 0; j < boardWidth; ++j) {
                possible[i][j] = false;
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for(int j = 0; j < boardWidth; ++j) {
                if (priorities[i][j] == 0 || possible[i][j]){
                    continue;
                } else {
                    if (i + shortest - 1 < boardHeight) {
                        boolean V = true;
                        for(int l = 0; l < shortest; ++l){
                            if (!state.get(i + l).get(j).equals("")){
                                V = false;
                                break;
                            }
                        }
                        if (V) {
                            for(int l = 0; l < shortest; ++l){
                                possible[i + l][j] = true;
                            }
                        }
                    }
                    if (j + shortest - 1 < boardWidth) {
                        boolean H = true;
                        for(int l = 0; l < shortest; ++l){
                            if (!state.get(i).get(j + l).equals("")){
                                H = false;
                                break;
                            }
                        }
                        if (H) {
                            for(int l = 0; l < shortest; ++l){
                                possible[i][j + l] = true;
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for(int j = 0; j < boardWidth; ++j) {
                if (!possible[i][j]) {
                    priorities[i][j] = 0.2f;
                }
            }
        }
    }

    private static Shot randomShot() {
        int maxCol = 0;
        int maxRow = 0;
        float maxP = 0;
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (priorities[i][j] > maxP) {
                    maxRow = i;
                    maxCol = j;
                    maxP = priorities[i][j];
                }
            }
        }
        return new Shot(maxRow, maxCol);
    }

    private static void updatePriority(ArrayList<ArrayList<String>> state) {
        for (int i = 0; i < boardHeight; ++i){
            for (int j = 0; j < boardWidth; ++j) {
                if (state.get(i).get(j).equals("H")) {
                    for(int k = 0; k < 4; ++k){
                        int newRow = i + localShape[k][0];
                        int newCol = j + localShape[k][1];
                        if (canShoot(newRow, newCol, state)) {
                            priorities[newRow][newCol] = min(priorities[newRow][newCol] + 0.4f, 1);
                        }
                    }
                } else if (state.get(i).get(j).equals("M")) {
                    for(int k = 0; k < 4; ++k){
                        int newRow = i + localShape[k][0];
                        int newCol = j + localShape[k][1];
                        if (canShoot(newRow, newCol, state)) {
                            priorities[newRow][newCol] = max(priorities[newRow][newCol] - 0.05f, 0.1f);
                        }
                    }
                }
            }
        }
    }

    private static BattleshipsMove executeS(ArrayList<ArrayList<String>> state) {
        switch (currentState) {
            case 1:
                updatePriority(state);
                lastShot = randomShot();
                break;
            case 2:
                lastShot = new Shot(trackOrigin.row - 1, trackOrigin.col);
                break;
            case 3:
                lastShot = new Shot(trackOrigin.row + 1, trackOrigin.col);
                break;
            case 4:
                lastShot = new Shot(trackOrigin.row, trackOrigin.col - 1);
                break;
            case 5:
                lastShot = new Shot(trackOrigin.row, trackOrigin.col + 1);
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
        return new BattleshipsMove(null, Character.toString((char) (lastShot.row + 65)), lastShot.col + 1);
    }

    private static boolean canShoot(int row, int col, ArrayList<ArrayList<String>> state) {
        if (row >= 0 && row < boardHeight && col >= 0 && col < boardWidth) {
            return priorities[row][col] != 0 && state.get(row).get(col).equals("");
        } else {
            return false;
        }
    }

    private static int shortestShipLength(ArrayList<ArrayList<String>> state) {
        HashSet<Integer> sunk = new HashSet<>();
        for (int i = 0; i < boardHeight; ++i){
            for(int j = 0; j < boardWidth; ++j) {
                if (state.get(i).get(j).contains("S")){
                    sunk.add(Integer.parseInt(state.get(i).get(j).substring(1)));
                }
            }
        }
        int shortest = 100;
        for(int i = 0; i < shipLengths.size(); ++i) {
            if (!sunk.contains(i) && shipLengths.get(i) < shortest){
                shortest = shipLengths.get(i);
            }
        }
        return shortest;
    }

    private static boolean available(int state, ArrayList<ArrayList<String>> board) {
        switch (state) {
            case 1:
                return true;
            case 2:
                return canShoot(trackOrigin.row - 1, trackOrigin.col, board);
            case 3:
                return canShoot(trackOrigin.row + 1, trackOrigin.col, board);
            case 4:
                return canShoot(trackOrigin.row, trackOrigin.col - 1, board);
            case 5:
                return canShoot(trackOrigin.row, trackOrigin.col + 1, board);
            case 6:
                return canShoot(lastShot.row - 1, lastShot.col, board);
            case 7:
                return canShoot(lastShot.row + 1, lastShot.col, board);
            case 8:
                return canShoot(lastShot.row, lastShot.col - 1, board);
            case 9:
                return canShoot(lastShot.row, lastShot.col + 1, board);
            default:
                return true;
        }
    }

    private static BattleshipsMove findShipWithStateMachine(MainWindow.GameState state) {
        if (currentState == 0) {
            currentState = 1;
            return executeS(state.OppBoard);
        } else {
            shotResult = state.OppBoard.get(lastShot.row).get(lastShot.col);
            if (shotResult.contains("S")){
                reducePossiblities(state.OppBoard);
                currentState = 1;
            } else if (shotResult.equals("H")) {
                if (currentState == 1) trackOrigin = new Shot(lastShot.row, lastShot.col);
                currentState = transitions[currentState][0];
            } else if (shotResult.equals("M")) {
                currentState = transitions[currentState][1];
            }
            while (!available(currentState, state.OppBoard)) {
                currentState = transitions[currentState][1];
                System.out.println(currentState);
            }
            return executeS(state.OppBoard);
        }
    }


    private static ArrayList<ArrayList<String>> attemptShipPlacement(int i, int j, ArrayList<ArrayList<String>> board, int length, String orientation, int shipNum) {
        if (orientation.equals("V")) { //Vertical
            if (i + length - 1 >= board.size()) { //If ship doesn't fit within board boundaries
                return null;
            }
            for (int l = 0; l < length; l++) {
                String square = board.get(i + l).get(j);
                if (!square.equals("")) {
                    return null;
                }
            }
            for (int l = 0; l < length; l++) {
                board.get(i + l).set(j, Integer.toString(shipNum));
            }
            return board;
        } else { //Horizontal
            if (j + length - 1 >= board.get(0).size()) { //If ship doesn't fit within board boundaries
                return null;
            }
            for (int l = 0; l < length; l++) {
                String square = board.get(i).get(j + l);
                if (!square.equals("")) {
                    return null;
                }
            }
            for (int l = 0; l < length; l++) {
                board.get(i).set(j + l, Integer.toString(shipNum));
            }
            return board;
        }
    }

    public static class ByScore implements Comparator<ShipPositionToBe> {
        public int compare(ShipPositionToBe a, ShipPositionToBe b) {
            return (new Integer(a.score)).compareTo(b.score);
        }
    }
    private static class ShipPositionToBe extends ShipPosition {
        int score;

        ShipPositionToBe(String row, int column, String orientation, int score) {
            super(row, column, orientation);
            this.score = score;
        }
    }

    private static class Shot {
        int row;
        int col;

        Shot(int a, int b) {
            row = a;
            col = b;
        }
    }

    private static void printMat(int[][] input) {
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                System.out.print(input[i][j]+"; ");
            }
            System.out.println();
        }
    }
}