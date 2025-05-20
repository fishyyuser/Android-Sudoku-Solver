import tensorflow as tf

# Load the saved .h5 model
model = tf.keras.models.load_model('sudoku_digit_model.h5')

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the converted model
with open('sudoku_digit_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("✅ Successfully converted to TFLite format!")
