<h1>📱 Sudoku Solver Mobile Application</h1>

<p><strong>Sudoku Solver</strong> is an Android-based mobile application that enables users to scan Sudoku puzzles using their smartphone camera and receive instant solutions. The app combines computer vision and digit recognition to extract the Sudoku grid from an image and solve it using an efficient backtracking algorithm—all within an intuitive and editable user interface.</p>

<h2>🔍 Key Features</h2>
<ul>
  <li><strong>Image-Based Grid Detection:</strong> Capture Sudoku puzzles using the device camera.</li>
  <li><strong>Digit Recognition:</strong> Recognize digits using a TensorFlow Lite model.</li>
  <li><strong>Editable Grid Interface:</strong> Manually correct any misrecognized digits.</li>
  <li><strong>Offline Solving:</strong> Solved on-device using a backtracking algorithm.</li>
  <li><strong>Flask Backend:</strong> Preprocessing and recognition hosted on AWS EC2.</li>
</ul>

<h2>🛠️ Tech Stack</h2>
<ul>
  <li><strong>Mobile App:</strong> Android (Java), Android Studio</li>
  <li><strong>Backend:</strong> Python, Flask, OpenCV, TensorFlow Lite</li>
  <li><strong>Hosting:</strong> AWS EC2</li>
  <li><strong>Model:</strong> CNN trained on Kaggle dataset (digits 1–9)</li>
</ul>

<h2>📦 Project Modules</h2>
<ul>
  <li><code>app/</code> - Android application source code</li>
  <li><code>server/</code> - Flask-based backend API</li>
  <li><code>model/</code> - CNN training scripts and exported TFLite model</li>
  <li><code>assets/</code> - Sample input images and outputs</li>
</ul>

<h2>📸 Example Flow</h2>
<ol>
  <li>User captures an image of a Sudoku puzzle.</li>
  <li>Image is sent to the Flask API hosted on EC2.</li>
  <li>Grid and digits are extracted using OpenCV and TFLite.</li>
  <li>Digits are returned and displayed in a 9×9 editable grid.</li>
  <li>The app solves the puzzle on-device using backtracking.</li>
</ol>

<h2>📈 Future Enhancements</h2>
<ul>
  <li>Support for handwritten puzzle recognition</li>
  <li>Offline image processing and digit recognition</li>
  <li>Improved robustness under low-light conditions</li>
</ul>
