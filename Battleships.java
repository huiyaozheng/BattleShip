package battleships;

import java.util.ArrayList;
import java.util.Random;

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

    /* Use this method to define the bot's strategy.
     */
    public static BattleshipsMove calculateMove(MainWindow.GameState state) {
        if (state.Round == 0) {
            boardHeight = state.MyBoard.size();
            boardWidth = state.MyBoard.get(0).size();
            priorities = new float[boardHeight][boardWidth];
            placement = new int[boardHeight][boardWidth];
            for (int i = 0; i < boardHeight; ++i) {
                for (int j = 0; j < boardWidth; ++j) {
                    if (state.MyBoard.get(i).get(j).equals("")) {
                        placement[i][j] = 100;
                    }
                    if (state.MyBoard.get(i).get(j).equals(""))
                        priorities[i][j] = abs(i - j) % 2 == 0 ? 0.5f : 0.3f;
                    else
                        priorities[i][j] = 0;
                }
            }
            currentState = 0;
            shotResult = "";
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
                if (able) result.add(new ShipPositionToBe(Character.toString((char) (i + 65)), j, "V", score));
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
                if (able) result.add(new ShipPositionToBe(Character.toString((char) (i + 65)), j, "H", score));
            }
        }
        return result;
    }

    private static ShipPosition findBestPosition(ArrayList<ShipPositionToBe> validPositions) {
        int max = 0;
        ShipPosition result = null;
        for (ShipPositionToBe i : validPositions) {
            if (i.score > max) {
                max = i.score;
                result = i;
            }
        }
        return result;
    }

    private static BattleshipsMove placeShips(MainWindow.GameState state) {
        ArrayList<ShipPosition> placement = new ArrayList<ShipPosition>();
        for (int i = 0; i < state.Ships.size(); i++) {
            ShipPosition newPlace = findBestPosition(findValidPosition(state.Ships.get(i), state.MyBoard));
            ArrayList<ArrayList<String>> board;
            board = attemptShipPlacement(newPlace.Row.charAt(0) - 'A', newPlace.Column, state.MyBoard, state.Ships.get(i), newPlace.Orientation, i);
            state.MyBoard = board;
            newPlace.Column++;
            placement.add(newPlace);
        }
        return new BattleshipsMove(placement, "", 0);
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
                        if (canShoot(newRow, newCol)) {
                            priorities[newRow][newCol] = min(priorities[newRow][newCol] + 0.1f, 1);
                        }
                    }
                } else if (state.get(i).get(j).equals("M")) {
                    for(int k = 0; k < 4; ++k){
                        int newRow = i + localShape[k][0];
                        int newCol = j + localShape[k][1];
                        if (canShoot(newRow, newCol)) {
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

    private static boolean canShoot(int row, int col) {
        if (row >= 0 && row < boardHeight && col >= 0 && col < boardWidth) {
            return priorities[row][col] != 0;
        } else {
            return false;
        }
    }

    private static boolean available(int state) {
        switch (state) {
            case 1:
                return true;
            case 2:
                return canShoot(trackOrigin.row - 1, trackOrigin.col);
            case 3:
                return canShoot(trackOrigin.row + 1, trackOrigin.col);
            case 4:
                return canShoot(trackOrigin.row, trackOrigin.col - 1);
            case 5:
                return canShoot(trackOrigin.row, trackOrigin.col + 1);
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
        if (currentState == 0) {
            currentState = 1;
            return executeS(state.OppBoard);
        } else {
            shotResult = state.OppBoard.get(lastShot.row).get(lastShot.col);
            if (shotResult.equals("H")) {
                if (currentState == 1) trackOrigin = new Shot(lastShot.row, lastShot.col);
                currentState = transitions[currentState][0];
            } else if (shotResult.equals("M")) {
                currentState = transitions[currentState][1];
            }
            while (!available(currentState)) {
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
}