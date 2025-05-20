package com.example.sudokusolver.SolverAlgo;

import com.example.sudokusolver.Models.SudokuGrid;

import java.util.ArrayList;
import java.util.List;

public class SudokuSolver {

    private static final int SIZE = 9;
    private int[][] solution = new int[SIZE][SIZE];
    private boolean foundSolution = false;
    private boolean hasMultipleSolutions = false;

    public boolean solveSudoku(SudokuGrid model) throws IllegalArgumentException {
        int[][] grid = convertToArray(model);

        if (!isValidGrid(grid)) {
            throw new IllegalArgumentException("Invalid Sudoku Grid: Violates Sudoku rules.");
        }

        foundSolution = false;
        hasMultipleSolutions = false;

        solve(grid, 0, 0);

        if (!foundSolution) {
            throw new IllegalArgumentException("No solution exists for this Sudoku.");
        }

        // Update the model with the solved grid
        model.setGrid(convertToList(solution));

        return hasMultipleSolutions;
    }

    private boolean solve(int[][] grid, int row, int col) {
        if (row == SIZE) {
            if (!foundSolution) {
                foundSolution = true;
                solution = deepCopyGrid(grid);
            } else {
                hasMultipleSolutions = true;
            }
            return false;
        }

        if (grid[row][col] != 0) {
            return solve(grid, nextRow(row, col), nextCol(col));
        }

        for (int num = 1; num <= 9; num++) {
            if (isSafe(grid, row, col, num)) {
                grid[row][col] = num;
                if (!solve(grid, nextRow(row, col), nextCol(col)) && hasMultipleSolutions) {
                    return false;
                }
                grid[row][col] = 0;
            }
        }

        return false;
    }

    private boolean isValidGrid(int[][] grid) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int value = grid[row][col];
                if (value != 0) {
                    grid[row][col] = 0;
                    if (!isSafe(grid, row, col, value)) {
                        return false;
                    }
                    grid[row][col] = value;
                }
            }
        }
        return true;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == num || grid[i][col] == num) {
                return false;
            }
        }

        int boxRow = row - row % 3;
        int boxCol = col - col % 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[boxRow + i][boxCol + j] == num) {
                    return false;
                }
            }
        }

        return true;
    }

    private int nextRow(int row, int col) {
        return (col == SIZE - 1) ? row + 1 : row;
    }

    private int nextCol(int col) {
        return (col + 1) % SIZE;
    }

    private int[][] deepCopyGrid(int[][] grid) {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }

    // Convert List<List<Integer>> to int[][]
    private int[][] convertToArray(SudokuGrid model) {
        List<List<Integer>> listGrid = model.getGrid();
        int[][] array = new int[SIZE][SIZE];

        for (int i = 0; i < SIZE; i++) {
            List<Integer> row = listGrid.get(i);
            for (int j = 0; j < SIZE; j++) {
                array[i][j] = row.get(j) != null ? row.get(j) : 0;
            }
        }

        return array;
    }

    // Convert int[][] back to List<List<Integer>>
    private List<List<Integer>> convertToList(int[][] array) {
        List<List<Integer>> listGrid = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < SIZE; j++) {
                row.add(array[i][j]);
            }
            listGrid.add(row);
        }
        return listGrid;
    }
}
