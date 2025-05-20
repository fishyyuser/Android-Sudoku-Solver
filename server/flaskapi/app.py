from flask import Flask, request, jsonify
import numpy as np
import cv2
import os
import warnings

# Suppress TensorFlow and Python warnings
os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
warnings.filterwarnings("ignore", category=UserWarning)

from sudoku_recognizer import recognize_grid_from_array

app = Flask(__name__)

@app.route("/recognize", methods=["POST"])
def recognize_sudoku():
    if 'image' not in request.files:
        return jsonify({"error": "No image uploaded"}), 400

    file = request.files['image']
    in_memory_file = file.read()
    np_img = np.frombuffer(in_memory_file, np.uint8)
    image = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

    if image is None:
        return jsonify({"error": "Invalid image file"}), 400

    # Resize with aspect ratio preservation and pad to 450x450
    h, w = image.shape[:2]
    scale = 450 / max(h, w)
    resized = cv2.resize(image, (int(w * scale), int(h * scale)))

    pad_h = 450 - resized.shape[0]
    pad_w = 450 - resized.shape[1]

    image = cv2.copyMakeBorder(
        resized, 0, pad_h, 0, pad_w,
        cv2.BORDER_CONSTANT, value=(0, 0, 0)
    )

    try:
        grid = recognize_grid_from_array(image)
        return jsonify({"grid": grid}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(debug=False, host="0.0.0.0", port=5000)
