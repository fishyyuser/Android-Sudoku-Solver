package com.example.sudokusolver;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.example.sudokusolver.ApiServices.IApiService;
import com.example.sudokusolver.ApiServices.RetrofitClient;
import com.example.sudokusolver.Models.SudokuGrid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sudokusolver.SolverAlgo.SudokuSolver;
import com.example.sudokusolver.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 102;
    private String currentPhotoPath;
    private EditText[][] sudokuCells = new EditText[9][9];
    private  SudokuGrid ApiResponse = new SudokuGrid();

    // for background running
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.mainLayout.setVisibility(View.GONE);
        SetGridCells();

        binding.buttonFirst.setOnClickListener(v -> {
            binding.startLayout.setVisibility(View.GONE);
            binding.mainLayout.setVisibility(View.VISIBLE);

        });

        binding.buttonCamera.setOnClickListener(v -> {
            checkCameraPermission();
        });
        binding.buttonUpload.setOnClickListener(v -> {
            checkPermissionsAndOpenFilePicker();
        });
        binding.buttonReset.setOnClickListener(v -> {
            if (ApiResponse != null){
                ApiResponse.resetGrid();
                updateGridFromModel();
            }
        });
        binding.buttonSolver.setOnClickListener(v -> {
            if (ApiResponse == null){
                return;
            }
            updateModelFromGrid(); // update ApiResponse based on user inputs
            binding.progressBar.setVisibility(View.VISIBLE); // Show spinner
            executor.execute(() -> {
                SudokuSolver solver = new SudokuSolver();
                boolean multipleSolutions;
                String error = null;

                try {
                    multipleSolutions = solver.solveSudoku(ApiResponse);
                } catch (IllegalArgumentException e) {
                    error = e.getMessage();
                    multipleSolutions = false;
                }

                boolean finalMultipleSolutions = multipleSolutions;
                String finalError = error;

                mainThreadHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE); // Hide spinner

                    if (finalError != null) {
                        Toast.makeText(MainActivity.this, "Error: " + finalError, Toast.LENGTH_LONG).show();
                    } else {
                        if (finalMultipleSolutions) {
                            Toast.makeText(MainActivity.this, "Multiple solutions found. Showing one.", Toast.LENGTH_LONG).show();
                        }
                        updateGridFromModel();
                    }
                });
            });
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }



    private void  SetGridCells(){        // set gird view
        int cellSize = dpToPx(38); // Each cell is 40dp
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                EditText cell = new EditText(this);
                cell.setLayoutParams(new ViewGroup.LayoutParams(cellSize, cellSize));
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setInputType(InputType.TYPE_CLASS_NUMBER);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)}); // Only 1 digit
                cell.setBackgroundResource(R.drawable.sudoku_cell_bg); // optional border
                sudokuCells[row][col] = cell; // store reference
                binding.sudokuGrid.addView(cell);
            }
        }
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private  void ApiCall(File file){
        if (ApiResponse != null){
            ApiResponse.resetGrid();
        }
        if (!file.exists() || file.length() == 0) {
            Toast.makeText(this, "Invalid file: not found or empty", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE); // Show spinner
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        IApiService apiService = RetrofitClient.getClient().create(IApiService.class);
        Call<SudokuGrid> call = apiService.uploadImage(imagePart);
        call.enqueue(new Callback<SudokuGrid>() {
            @Override
            public void onResponse(Call<SudokuGrid> call, Response<SudokuGrid> response) {
                if (response.isSuccessful()) {
                    ApiResponse = response.body();
                    // bind output with UI
                    updateGridFromModel();

                } else {
                    Toast.makeText(MainActivity.this,  "API_ERROR Code: " + response.code(), Toast.LENGTH_LONG).show();
                }
                binding.progressBar.setVisibility(View.GONE); // Show spinner
            }
            @Override
            public void onFailure(Call<SudokuGrid> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE); // Show spinner
                Toast.makeText(MainActivity.this,  t.getMessage(), Toast.LENGTH_LONG).show();
                updateGridFromModel();
            }
        });
    }
    // camera open
    private void dispatchTakePictureIntent() {
        // 1. Create the camera intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Special handling for some Chinese manufacturers
//        if (Build.MANUFACTURER.equalsIgnoreCase("huawei") ||
//                Build.MANUFACTURER.equalsIgnoreCase("honor")) {
//            takePictureIntent.setPackage("com.huawei.camera");
//        } else if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
//            takePictureIntent.setPackage("com.android.camera");
//        }

//        // 2. Ensure there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
//            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
//            return;
//        }

        // 3. Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);

            // 5. Add the URI as output and grant permissions
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 6. Start the camera activity
           // startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            cameraActivityResultLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Read the temporary file
                        File tempFile = new File(currentPhotoPath);
                        // Upload to API
                        ApiCall(tempFile);
                        Toast.makeText(MainActivity.this, "Image saved to: " + currentPhotoPath, Toast.LENGTH_SHORT).show();
                        // Delete the temporary file
                       // tempFile.delete();
                    }else {
                        Toast.makeText(MainActivity.this, "Image capture failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    // Camera permission
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // storage permission
    private void checkPermissionsAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openFilePicker();
            }
        } else {
            // For older versions, use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openFilePicker();
            }
        }
    }
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); // You can change this to a specific MIME type, e.g., "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"));
    }
    ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri fileUri = result.getData().getData();
            try {
                File selectedFile = createTempFileFromUri(fileUri);
                ApiCall(selectedFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
);
    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;
        // Create temp file in cache directory
        File tempFile = File.createTempFile("upload_", ".tmp", getCacheDir());
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4 * 1024]; // 4KB buffer
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private void updateGridFromModel() {
        if (ApiResponse == null){
            return;
        }
        List<List<Integer>> gridData = ApiResponse.getGrid();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Integer value = gridData.get(row).get(col);
                if (value != null && value != 0) {
                    sudokuCells[row][col].setText(String.valueOf(value));
                } else {
                    sudokuCells[row][col].setText(""); // Clear empty cells
                }
            }
        }
    }

    private void updateModelFromGrid() {
        if (ApiResponse == null) {
            return;
        }
        List<List<Integer>> gridData = ApiResponse.getGrid();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                String cellText = sudokuCells[row][col].getText().toString().trim();
                int value = 0;
                if (!cellText.isEmpty()) {
                    try {
                        value = Integer.parseInt(cellText);
                        if (value < 1 || value > 9) {
                            value = 0; // Optional: ignore invalid numbers
                        }
                    } catch (NumberFormatException e) {
                        value = 0; // Non-numeric input is treated as empty
                    }
                }
                gridData.get(row).set(col, value);
            }
        }
    }

}