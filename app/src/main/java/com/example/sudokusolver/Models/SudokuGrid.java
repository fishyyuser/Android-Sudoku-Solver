package com.example.sudokusolver.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SudokuGrid {
    private List<List<Integer>> grid;
    public SudokuGrid() {
        this.grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            List<Integer> row = new ArrayList<>(Collections.nCopies(9, 0));
            grid.add(row);
        }
    }

    public SudokuGrid(List<List<Integer>> grid) {
        this.grid = grid;
    }

    public List<List<Integer>> getGrid() {
        return grid;
    }

    public void setGrid(List<List<Integer>> grid) {
        this.grid = grid;
    }
    public  void resetGrid(){
        if (this.grid != null) {
            for (List<Integer> row : grid) {
                for (int i = 0; i < row.size(); i++) {
                    row.set(i, 0);
                }
            }
        }

    }
}
