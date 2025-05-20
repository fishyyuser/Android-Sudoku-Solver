# sudoku_solver.py
import numpy as np

def is_valid_sudoku(grid):
    """Check if the Sudoku grid follows the rules"""
    def is_valid_unit(unit):
        nums = [num for num in unit if num != 0]
        return len(nums) == len(set(nums))

    # Check rows and columns
    for i in range(9):
        if not is_valid_unit(grid[i]) or not is_valid_unit(grid[:, i]):
            return False

    # Check 3x3 subgrids
    for i in range(0, 9, 3):
        for j in range(0, 9, 3):
            subgrid = grid[i:i+3, j:j+3].flatten()
            if not is_valid_unit(subgrid):
                return False
    return True

def find_empty(grid):
        for i in range(9):
            for j in range(9):
                if grid[i][j] == 0:
                    return (i, j)
        return None

def is_safe(grid, row, col, num):
        # Check row, column, and subgrid
        return (num not in grid[row] and
                num not in grid[:, col] and
                num not in grid[row//3*3:row//3*3+3, col//3*3:col//3*3+3])

        backtrack(grid)
        return len(solutions) == 1

def solve_sudoku(grid):
    """Solve the Sudoku in-place and return True if a unique solution exists."""
    solutions = []
    grid_copy = np.copy(grid)  # Backup to reset after checking uniqueness
    
    def backtrack(grid):
        empty = find_empty(grid)
        if not empty:
            solutions.append(np.copy(grid))
            return len(solutions) > 1  # Stop if multiple solutions
        
        row, col = empty
        for num in range(1, 10):
            if is_safe(grid, row, col, num):
                grid[row][col] = num
                if backtrack(grid):
                    return True
                grid[row][col] = 0
        return False
    
    # Find all solutions
    backtrack(grid_copy)
    
    if len(solutions) == 1:
        # Update the original grid with the solution
        grid[:] = solutions[0]
        return True
    return False

    

    

def print_grid(grid):
    """Pretty-print the Sudoku grid"""
    for i, row in enumerate(grid):
        if i % 3 == 0 and i != 0:
            print("-" * 25)
        row_str = " | ".join(
            " ".join(str(num) if num != 0 else "." for num in row[j*3:(j+1)*3]) 
            for j in range(3)
        )
        print(f" {row_str} ")