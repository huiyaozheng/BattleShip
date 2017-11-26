package battleships;

import java.util.*;

import static java.lang.Math.PI;
import static java.lang.Math.round;

public class Battleships {
    /**
     * Random number generator for our auto play game strategy.
     */
    static Random r = new Random();

    private static float[][] priorities = null;
    // Priority is a float in [0,1]
    // Priority is -1 means cannot shoot at the cell
    private static int boardWidth;
    private static int boardHeight;
    private static int[][] placement = null;
    private static Shot lastShot = null;
    private static int[][] localShape = {{-1, 0}, {0, -1}, {0, 1}, {1, 0},{-1,-1},{-1,1},{1,-1},{1,1}};
    private static ArrayList<Integer> shipLengths;
    private static int[][] startingPosCount;
    private static HashSet<Integer> remainShipInd;

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
                        placement[i][j] = 100;
                    }
                }
            }
            shipLengths = state.Ships;
            remainShipInd = new HashSet<>();
            for(int i = 0; i < state.Ships.size(); ++i) remainShipInd.add(i);
            calculatePriority(state, remainShipInd);
            return placeShips(state);
        } else {
            return findShipWithPriority(state);
        }
    }

    private static void calculatePriority(MainWindow.GameState state, HashSet<Integer> remainShipIndices) {
        for (int k : remainShipIndices) {
            int length = state.Ships.get(k);
            for (int i = 0; i < boardHeight - length + 1; i++) {
                for (int j = 0; j < boardWidth; j++) {
                    boolean able = true;
                    for (int l = 0; l < length; l++) {
                        String square = state.OppBoard.get(i + l).get(j);
                        if (!square.equals("")) {
                            able = false;
                            break;
                        }
                    }
                    if (able) {
                        for (int l = 0; l < length; l++) {
                            startingPosCount[i + l][j]++;
                        }
                    }
                }
            }
            for (int i = 0; i < boardHeight; i++) {
                for (int j = 0; j < boardWidth - length + 1; j++) {
                    boolean able = true;
                    for (int l = 0; l < length; l++) {
                        String square = state.OppBoard.get(i).get(j + l);
                        if (!square.equals("")) {
                            able = false;
                            break;
                        }
                    }
                    if (able) {
                        for (int l = 0; l < length; l++) {
                            startingPosCount[i][j + l]++;
                        }
                    }
                }
            }
        }
        int maxP = 0;
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (startingPosCount[i][j] > maxP) maxP = startingPosCount[i][j];
            }
        }
        maxP++;
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (state.MyBoard.get(i).get(j).equals("")) {
                    priorities[i][j] = startingPosCount[i][j] / ((float) maxP);
                } else {
                    priorities[i][j] = -1;
                }
            }
        }
        printMat(priorities);
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
        Collections.sort(validPositions, new ByScore());
        System.out.println();
        int length = validPositions.size();
        if (length == 1 || validPositions.get(length - 1).score > validPositions.get(length - 2).score) {
            return validPositions.get(length - 1);
        }
        int k = 1;
        for (int i = validPositions.size() - 2; i >= 0; --i) {
            if (validPositions.get(i).score == validPositions.get(i + 1).score) {
                k++;
            } else {
                break;
            }
        }
        int lucky = r.nextInt(k);
        return validPositions.get(length - 1 - lucky);
    }

    private static BattleshipsMove placeShips(MainWindow.GameState state) {
        int adPenalty = 30;
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
            state.MyBoard = board;
            newPlace.Column++;
            placements.add(newPlace);
        }
        return new BattleshipsMove(placements, "", 0);
    }

    private static void reducePossiblities(ArrayList<ArrayList<String>> state) {
        // If a cell cannot contain ship, set its priority to 0.
        if (lastShot != null) {
            if (state.get(lastShot.row).get(lastShot.col).contains("S")){
                Integer i = Integer.parseInt(state.get(lastShot.row).get(lastShot.col).substring(1));
                for(Integer j : remainShipInd) {
                    if (j.equals(i)) {
                        remainShipInd.remove(j);
                        break;
                    }
                }
            }
        }
        int shortest = shortestShipLength(state);
        boolean possible[][] = new boolean[boardHeight][boardWidth];
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                possible[i][j] = false;
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (priorities[i][j] < 0 || possible[i][j]) {
                    continue;
                } else {
                    if (i + shortest - 1 < boardHeight) {
                        boolean V = true;
                        for (int l = 0; l < shortest; ++l) {
                            if (!state.get(i + l).get(j).equals("")) {
                                V = false;
                                break;
                            }
                        }
                        if (V) {
                            for (int l = 0; l < shortest; ++l) {
                                possible[i + l][j] = true;
                            }
                        }
                    }
                    if (j + shortest - 1 < boardWidth) {
                        boolean H = true;
                        for (int l = 0; l < shortest; ++l) {
                            if (!state.get(i).get(j + l).equals("")) {
                                H = false;
                                break;
                            }
                        }
                        if (H) {
                            for (int l = 0; l < shortest; ++l) {
                                possible[i][j + l] = true;
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (!possible[i][j]) {
                    priorities[i][j] = -1;
                }
            }
        }
    }

    private static Shot randomShot(ArrayList<ArrayList<String>> state) {
        boolean hasHitCells = false;
        int[][] nextShot = new int[boardHeight][boardWidth];
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                nextShot[i][j] = 0;
            }
        }
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (state.get(i).get(j).equals("H")) {
                    hasHitCells = true;
                    for (int k = 0; k < 4; ++k) {
                        int newRow = i + localShape[k][0];
                        int newCol = j + localShape[k][1];
                        if (canShoot(newRow, newCol, state)) {
                            nextShot[newRow][newCol] += 3;
                        }
                        newRow = i + localShape[k][0] * 2;
                        newCol = j + localShape[k][1] * 2;
                        if (canShoot(newRow, newCol, state)) {
                            nextShot[newRow][newCol] += 2;
                        }
                    }
                }
            }
        }

        if (hasHitCells) {
            int maxCol = 0;
            int maxRow = 0;
            int maxP = 0;
            for (int i = 0; i < boardHeight; ++i) {
                for (int j = 0; j < boardWidth; ++j) {
                    if (nextShot[i][j] > maxP && state.get(i).get(j).equals("")) {
                        maxRow = i;
                        maxCol = j;
                        maxP = nextShot[i][j];
                    }
                }
            }
            return new Shot(maxRow, maxCol);
        } else {
            for (int i = 0; i < boardHeight; ++i) {
                for (int j = 0; j < boardWidth; ++j) {
                    if (state.get(i).get(j).equals("M")) {
                        System.out.println("Entered");
                        for (int k = 0; k < 8; ++k) {
                            int newRow = i + localShape[k][0];
                            int newCol = j + localShape[k][1];
                            if (canShoot(newRow, newCol, state)) {
                                nextShot[newRow][newCol] += 5;
                            }
                            newRow = i + localShape[k][0] * 2;
                            newCol = j + localShape[k][1] * 2;
                            if (canShoot(newRow, newCol, state)) {
                                nextShot[newRow][newCol] += 3;
                            }
                        }
                    }
                    if(priorities[i][j] < 0) {
                        nextShot[i][j] = 1000;
                        continue;
                    }
                    nextShot[i][j] -= round(priorities[i][j] * 10f);
                }
            }
            int minCol = 0;
            int minRow = 0;
            int minP = 1000000;
            for (int i = 0; i < boardHeight; ++i) {
                for (int j = 0; j < boardWidth; ++j) {
                    if (nextShot[i][j] < minP && state.get(i).get(j).equals("")) {
                        minRow = i;
                        minCol = j;
                        minP = nextShot[i][j];
                    }
                }
            }
            printMat(state);
            printMat(nextShot);
            return new Shot(minRow, minCol);
        }
    }

    private static BattleshipsMove executeS(ArrayList<ArrayList<String>> state) {
        lastShot = randomShot(state);
        System.out.print("Fire at ");
        System.out.print(lastShot.row);
        System.out.println(lastShot.col);
        priorities[lastShot.row][lastShot.col] = 0;
        return new BattleshipsMove(null, Character.toString((char) (lastShot.row + 65)), lastShot.col + 1);
    }

    private static boolean canShoot(int row, int col, ArrayList<ArrayList<String>> state) {
        if (row >= 0 && row < boardHeight && col >= 0 && col < boardWidth) {
            return state.get(row).get(col).equals("");
        } else {
            return false;
        }
    }

    private static int shortestShipLength(ArrayList<ArrayList<String>> state) {
        HashSet<Integer> sunk = new HashSet<>();
        for (int i = 0; i < boardHeight; ++i) {
            for (int j = 0; j < boardWidth; ++j) {
                if (state.get(i).get(j).contains("S")) {
                    sunk.add(Integer.parseInt(state.get(i).get(j).substring(1)));
                }
            }
        }
        int shortest = 100;
        for (int i = 0; i < shipLengths.size(); ++i) {
            if (!sunk.contains(i) && shipLengths.get(i) < shortest) {
                shortest = shipLengths.get(i);
            }
        }
        return shortest;
    }

    private static BattleshipsMove findShipWithPriority(MainWindow.GameState state) {
        reducePossiblities(state.OppBoard);

        return executeS(state.OppBoard);
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

    private static void printMat(int[][] input) {
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                System.out.print(input[i][j] + "; ");
            }
            System.out.println();
        }
    }

    private static void printMat(float[][] input) {
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                System.out.print(input[i][j] + "; ");
            }
            System.out.println();
        }
    }

    private static void printMat(ArrayList<ArrayList<String>> input) {
        for (int i = 0; i < input.size(); i++) {
            for (int j = 0; j < input.get(0).size(); j++) {
                System.out.print(input.get(i).get(j) + "; ");
            }
            System.out.println();
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
}