package org.example;

import com.badlogic.gdx.math.MathUtils;

import java.util.*;

/**
 * Robust 9x9 Sudoku generator, solver, validator, and state manager.
 * Supports multiple difficulty levels, candidate pencil notes, undo history, and hint system.
 */
public class SudokuEngine {

    public enum Difficulty {
        EASY("EASY", 36),
        MEDIUM("MEDIUM", 46),
        HARD("HARD", 52),
        EXPERT("EXPERT", 56);

        public final String label;
        public final int cluesToRemove;

        Difficulty(String label, int cluesToRemove) {
            this.label = label;
            this.cluesToRemove = cluesToRemove;
        }
    }

    public static class Move {
        public final int row;
        public final int col;
        public final int prevValue;
        public final int newValue;
        public final boolean[] prevNotes;

        public Move(int row, int col, int prevValue, int newValue, boolean[] prevNotes) {
            this.row = row;
            this.col = col;
            this.prevValue = prevValue;
            this.newValue = newValue;
            this.prevNotes = prevNotes != null ? Arrays.copyOf(prevNotes, 9) : new boolean[9];
        }
    }

    private final int[][] solution = new int[9][9];
    private final int[][] initial = new int[9][9];
    private final int[][] board = new int[9][9];
    private final boolean[][][] notes = new boolean[9][9][9]; // notes[r][c][digit-1]
    private final boolean[][] conflicts = new boolean[9][9];

    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    private final Stack<Move> history = new Stack<>();
    private int mistakes = 0;
    private final int maxMistakes = 3;
    private boolean pencilMode = false;
    private float elapsedTime = 0f;
    private boolean isCompleted = false;
    private boolean isGameOver = false;

    public SudokuEngine() {
        startNewGame(Difficulty.MEDIUM);
    }

    public void startNewGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.history.clear();
        this.mistakes = 0;
        this.elapsedTime = 0f;
        this.isCompleted = false;
        this.isGameOver = false;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                solution[r][c] = 0;
                initial[r][c] = 0;
                board[r][c] = 0;
                conflicts[r][c] = false;
                Arrays.fill(notes[r][c], false);
            }
        }

        // 1. Generate full solved board
        fillDiagonalBoxes();
        solveBoard(solution);

        // 2. Copy solution to initial and board
        for (int r = 0; r < 9; r++) {
            System.arraycopy(solution[r], 0, initial[r], 0, 9);
            System.arraycopy(solution[r], 0, board[r], 0, 9);
        }

        // 3. Remove digits according to difficulty
        removeDigits(difficulty.cluesToRemove);
        updateConflicts();
    }

    private void fillDiagonalBoxes() {
        for (int i = 0; i < 9; i += 3) {
            fillBox(i, i);
        }
    }

    private void fillBox(int row, int col) {
        List<Integer> digits = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(digits);
        int idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                solution[row + r][col + c] = digits.get(idx++);
            }
        }
    }

    private boolean solveBoard(int[][] grid) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] == 0) {
                    List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
                    Collections.shuffle(nums);
                    for (int num : nums) {
                        if (isValidPlacement(grid, r, c, num)) {
                            grid[r][c] = num;
                            if (solveBoard(grid)) {
                                return true;
                            }
                            grid[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPlacement(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == num && i != col) return false;
            if (grid[i][col] == num && i != row) return false;
        }
        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int currR = boxRow + r;
                int currC = boxCol + c;
                if ((currR != row || currC != col) && grid[currR][currC] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private void removeDigits(int count) {
        List<Integer> positions = new ArrayList<>(81);
        for (int i = 0; i < 81; i++) positions.add(i);
        Collections.shuffle(positions);

        int removed = 0;
        for (int pos : positions) {
            if (removed >= count) break;
            int r = pos / 9;
            int c = pos % 9;
            if (board[r][c] != 0) {
                board[r][c] = 0;
                initial[r][c] = 0;
                removed++;
            }
        }
    }

    public boolean setNumber(int row, int col, int num) {
        if (row < 0 || row >= 9 || col < 0 || col >= 9) return false;
        if (initial[row][col] != 0 || isCompleted || isGameOver) return false;

        if (pencilMode && num >= 1 && num <= 9) {
            // Toggle pencil candidate note
            boolean[] prevNotes = Arrays.copyOf(notes[row][col], 9);
            notes[row][col][num - 1] = !notes[row][col][num - 1];
            history.push(new Move(row, col, board[row][col], board[row][col], prevNotes));
            return true;
        }

        int prevVal = board[row][col];
        if (prevVal == num) return false; // No change

        boolean[] prevNotes = Arrays.copyOf(notes[row][col], 9);
        board[row][col] = num;
        Arrays.fill(notes[row][col], false); // clear notes if number placed
        history.push(new Move(row, col, prevVal, num, prevNotes));

        // Auto remove this note from row, col, and 3x3 box
        if (num > 0) {
            clearCandidateFromPeers(row, col, num);
        }

        updateConflicts();

        // Check if placed number is wrong
        if (num != 0 && num != solution[row][col]) {
            mistakes++;
            if (mistakes >= maxMistakes) {
                isGameOver = true;
            }
        }

        checkVictory();
        return true;
    }

    private void clearCandidateFromPeers(int row, int col, int num) {
        int idx = num - 1;
        for (int i = 0; i < 9; i++) {
            notes[row][i][idx] = false;
            notes[i][col][idx] = false;
        }
        int bR = (row / 3) * 3;
        int bC = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                notes[bR + r][bC + c][idx] = false;
            }
        }
    }

    public boolean erase(int row, int col) {
        if (row < 0 || row >= 9 || col < 0 || col >= 9) return false;
        if (initial[row][col] != 0 || isCompleted || isGameOver) return false;

        int prevVal = board[row][col];
        boolean hasNotes = false;
        for (boolean n : notes[row][col]) {
            if (n) { hasNotes = true; break; }
        }

        if (prevVal == 0 && !hasNotes) return false;

        boolean[] prevNotes = Arrays.copyOf(notes[row][col], 9);
        board[row][col] = 0;
        Arrays.fill(notes[row][col], false);
        history.push(new Move(row, col, prevVal, 0, prevNotes));
        updateConflicts();
        return true;
    }

    public boolean undo() {
        if (history.isEmpty() || isCompleted || isGameOver) return false;
        Move m = history.pop();
        board[m.row][m.col] = m.prevValue;
        System.arraycopy(m.prevNotes, 0, notes[m.row][m.col], 0, 9);
        updateConflicts();
        return true;
    }

    public boolean giveHint(int row, int col) {
        if (row < 0 || row >= 9 || col < 0 || col >= 9) return false;
        if (initial[row][col] != 0 || board[row][col] == solution[row][col]) return false;
        if (isCompleted || isGameOver) return false;

        setNumber(row, col, solution[row][col]);
        return true;
    }

    public void updateConflicts() {
        for (int r = 0; r < 9; r++) {
            Arrays.fill(conflicts[r], false);
        }

        // Row conflicts
        for (int r = 0; r < 9; r++) {
            for (int c1 = 0; c1 < 9; c1++) {
                int val = board[r][c1];
                if (val == 0) continue;
                for (int c2 = c1 + 1; c2 < 9; c2++) {
                    if (board[r][c2] == val) {
                        conflicts[r][c1] = true;
                        conflicts[r][c2] = true;
                    }
                }
            }
        }

        // Col conflicts
        for (int c = 0; c < 9; c++) {
            for (int r1 = 0; r1 < 9; r1++) {
                int val = board[r1][c];
                if (val == 0) continue;
                for (int r2 = r1 + 1; r2 < 9; r2++) {
                    if (board[r2][c] == val) {
                        conflicts[r1][c] = true;
                        conflicts[r2][c] = true;
                    }
                }
            }
        }

        // 3x3 Box conflicts
        for (int bR = 0; bR < 9; bR += 3) {
            for (int bC = 0; bC < 9; bC += 3) {
                for (int i = 0; i < 9; i++) {
                    int r1 = bR + i / 3;
                    int c1 = bC + i % 3;
                    int val = board[r1][c1];
                    if (val == 0) continue;
                    for (int j = i + 1; j < 9; j++) {
                        int r2 = bR + j / 3;
                        int c2 = bC + j % 3;
                        if (board[r2][c2] == val) {
                            conflicts[r1][c1] = true;
                            conflicts[r2][c2] = true;
                        }
                    }
                }
            }
        }
    }

    public void checkVictory() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0 || board[r][c] != solution[r][c]) {
                    return;
                }
            }
        }
        isCompleted = true;
    }

    public int getRemainingCount(int num) {
        int count = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == num) {
                    count++;
                }
            }
        }
        return Math.max(0, 9 - count);
    }

    public void updateTimer(float dt) {
        if (!isCompleted && !isGameOver) {
            elapsedTime += dt;
        }
    }

    public void restoreGame(Difficulty difficulty, int[][] savedBoard, int[][] savedInitial,
                            int[][] savedSolution, boolean[][][] savedNotes, float savedTime, int savedMistakes) {
        this.currentDifficulty = difficulty;
        this.history.clear();
        this.mistakes = savedMistakes;
        this.elapsedTime = savedTime;
        this.isCompleted = false;
        this.isGameOver = (mistakes >= maxMistakes);

        for (int r = 0; r < 9; r++) {
            System.arraycopy(savedBoard[r], 0, this.board[r], 0, 9);
            System.arraycopy(savedInitial[r], 0, this.initial[r], 0, 9);
            System.arraycopy(savedSolution[r], 0, this.solution[r], 0, 9);
            for (int c = 0; c < 9; c++) {
                System.arraycopy(savedNotes[r][c], 0, this.notes[r][c], 0, 9);
            }
        }
        updateConflicts();
        checkVictory();
    }

    public float getElapsedTime() {
        return elapsedTime;
    }

    public String getFormattedTime() {
        int totalSec = (int) elapsedTime;
        int minutes = totalSec / 60;
        int seconds = totalSec % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Getters and Setters
    public int getCell(int r, int c) { return board[r][c]; }
    public int getInitialCell(int r, int c) { return initial[r][c]; }
    public int getSolutionCell(int r, int c) { return solution[r][c]; }
    public boolean isConflict(int r, int c) { return conflicts[r][c]; }
    public boolean hasNote(int r, int c, int digit) { return notes[r][c][digit - 1]; }
    public boolean isPencilMode() { return pencilMode; }
    public void togglePencilMode() { pencilMode = !pencilMode; }
    public Difficulty getDifficulty() { return currentDifficulty; }
    public int getMistakes() { return mistakes; }
    public int getMaxMistakes() { return maxMistakes; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isGameOver() { return isGameOver; }
    public boolean canUndo() { return !history.isEmpty(); }
}
