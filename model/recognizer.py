import os
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'  # Disables oneDNN warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'    # Suppresses TensorFlow INFO messages
import warnings
warnings.filterwarnings("ignore", category=UserWarning)  # General Python warnings

import cv2
import numpy as np
import tensorflow.lite as tflite
import concurrent.futures


# --- Global variables for process-local TFLite interpreter ---
process_local_interpreter = None
process_input_details = None
process_output_details = None

def init_process():
    """Initialize TFLite interpreter once per process"""
    global process_local_interpreter, process_input_details, process_output_details
    try:
        process_local_interpreter = tflite.Interpreter(model_path="sudoku_digit_model.tflite")
        process_local_interpreter.allocate_tensors()
        process_input_details = process_local_interpreter.get_input_details()
        process_output_details = process_local_interpreter.get_output_details()
        
        # Verify model input shape
        assert tuple(process_input_details[0]['shape']) == (1, 32, 32, 1), \
            "Model expects (1, 32, 32, 1) input!"
            
        # Warm-up inference to avoid cold start latency
        dummy_input = np.zeros((1, 32, 32, 1), dtype=np.float32)  # Shape: (1, 32, 32, 1)
        process_local_interpreter.set_tensor(process_input_details[0]['index'], dummy_input)
        process_local_interpreter.invoke()
    except Exception as e:
        print(f"Process initialization failed: {e}")
        raise

# --- Create output directories ---
os.makedirs("output/cells", exist_ok=True)

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

def crop_cell(image):
    """Remove grid line borders from cell"""
    return image[4:-4, 4:-4]

def preprocess(image):
    """Convert to grayscale and adaptive thresholding"""
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (3, 3), 3)
    threshold_img = cv2.adaptiveThreshold(
        blur, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY_INV, 11, 2
    )
    cv2.imwrite("output/preprocessed.jpg", threshold_img)
    return threshold_img

def find_biggest_contour(image, original):
    """Find largest 4-point contour in threshold image"""
    contours, _ = cv2.findContours(image, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    biggest = None
    max_area = 0

    for contour in contours:
        area = cv2.contourArea(contour)
        if area > 500:
            peri = cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, 0.02 * peri, True)
            if len(approx) == 4 and area > max_area:
                biggest = approx
                max_area = area

    debug_image = original.copy()
    cv2.drawContours(debug_image, contours, -1, (0, 255, 0), 2)
    if biggest is not None:
        cv2.drawContours(debug_image, [biggest], -1, (0, 0, 255), 3)
    cv2.imwrite("output/contours_detected.jpg", debug_image)

    return biggest

def warp_grid(image, biggest_contour):
    """Perspective transform to square grid"""
    if biggest_contour is None:
        return None

    points = biggest_contour.reshape(4, 2).astype(np.float32)
    rect = np.zeros((4, 2), dtype=np.float32)
    
    # Order points: top-left, top-right, bottom-left, bottom-right
    s = points.sum(axis=1)
    rect[0] = points[np.argmin(s)]  # Top-left
    rect[3] = points[np.argmax(s)]  # Bottom-right
    
    d = np.diff(points, axis=1)
    rect[1] = points[np.argmin(d)]  # Top-right
    rect[2] = points[np.argmax(d)]  # Bottom-left

    # Destination points for perspective transform
    dst = np.float32([[0, 0], [450, 0], [0, 450], [450, 450]])
    matrix = cv2.getPerspectiveTransform(rect, dst)
    warped = cv2.warpPerspective(image, matrix, (450, 450))
    cv2.imwrite("output/warped_grid.jpg", warped)
    return warped

def extract_cells_from_grid(grid_image):
    """Split warped grid into 81 cells"""
    h, w = grid_image.shape[:2]
    cell_size = h // 9
    return [
        [crop_cell(grid_image[i*cell_size:(i+1)*cell_size, j*cell_size:(j+1)*cell_size])
         for j in range(9)]
        for i in range(9)
    ]

def center_and_resize(cell):
    """Center digit and resize to 32x32"""
    coords = cv2.findNonZero(cell)
    if coords is not None:
        x, y, w, h = cv2.boundingRect(coords)
        cell = cell[y:y+h, x:x+w]

    h, w = cell.shape
    pad_vert = (max(h, w) - h) // 2
    pad_horz = (max(h, w) - w) // 2
    cell = cv2.copyMakeBorder(cell, pad_vert, pad_vert, pad_horz, pad_horz,
                             cv2.BORDER_CONSTANT, value=0)
    return cv2.resize(cell, (32, 32), interpolation=cv2.INTER_AREA)

def recognize_digit(cell):
    """Preprocess cell and run TFLite inference"""
    global process_local_interpreter, process_input_details, process_output_details
    
    try:
        # Convert to grayscale if needed
        if len(cell.shape) == 3:
            cell = cv2.cvtColor(cell, cv2.COLOR_BGR2GRAY)
            
        # Threshold and center
        _, cell = cv2.threshold(cell, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
        processed_cell = center_and_resize(cell)
        
        # Normalize and add batch/channel dimensions
        normalized = (processed_cell / 255.0).astype(np.float32)
        model_input = np.expand_dims(normalized, axis=(0, -1))  # Shape: (1, 32, 32, 1)
        
        # Run inference
        process_local_interpreter.set_tensor(process_input_details[0]['index'], model_input)
        process_local_interpreter.invoke()
        output = process_local_interpreter.get_tensor(process_output_details[0]['index'])
        
        digit = np.argmax(output)
        confidence = np.max(output)
        return digit if confidence > 0.85 else 0
    except Exception as e:
        print(f"Digit recognition failed: {e}")
        return 0

def is_blank(cell):
    """Check if cell is empty using white pixel ratio"""
    white_pixels = cv2.countNonZero(cell)
    return (white_pixels / cell.size) < 0.02

def process_cell(cell):
    """Wrapper for parallel processing"""
    gray_cell = cv2.cvtColor(cell, cv2.COLOR_BGR2GRAY)
    _, threshold_cell = cv2.threshold(gray_cell, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
    return 0 if is_blank(threshold_cell) else recognize_digit(threshold_cell)


def process_sudoku(image_path):
    """Main processing pipeline"""
    if not os.path.exists(image_path):
        raise FileNotFoundError(f"Image {image_path} not found!")
    if not os.path.exists("sudoku_digit_model.tflite"):
        raise FileNotFoundError("TFLite model not found!")

    # Load and resize image with aspect ratio preservation
    image = cv2.imread(image_path)
    h, w = image.shape[:2]
    scale = 450 / max(h, w)
    resized = cv2.resize(image, (int(w*scale), int(h*scale)))
    
    # Pad to 450x450
    pad_h = 450 - resized.shape[0]
    pad_w = 450 - resized.shape[1]
    image = cv2.copyMakeBorder(resized, 0, pad_h, 0, pad_w, 
                              cv2.BORDER_CONSTANT, value=(0, 0, 0))
    cv2.imwrite("output/resized.jpg", image)

    # Processing pipeline
    preprocessed = preprocess(image)
    biggest_contour = find_biggest_contour(preprocessed, image)
    
    if biggest_contour is None:
        print("Sudoku grid not found!")
        return None

    warped_grid = warp_grid(image, biggest_contour)
    cells = extract_cells_from_grid(warped_grid)

    # Parallel cell processing
    sudoku_grid = np.zeros((9, 9), dtype=int)
    with concurrent.futures.ProcessPoolExecutor(initializer=init_process) as executor:
        cell_list = [cell for row in cells for cell in row]
        results = executor.map(process_cell, cell_list)
        
        for idx, result in enumerate(results):
            i, j = divmod(idx, 9)
            sudoku_grid[i, j] = result

    print("🔢 Recognized Sudoku Grid:")
    print_grid(sudoku_grid)

if __name__ == "__main__":
    try:
        sudoku_grid = process_sudoku("su.jpg")
        if sudoku_grid is not None:
            print("\nFinal Grid:")
            print_grid(sudoku_grid)
    except Exception as e:
        print(f"Error: {e}")