package com.mh.dominantcolor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;
    private ImageView imageView;
    private TextView colorCodeTextView;
    private TextView colorCodeTextRGBView;
    private TextToSpeech textToSpeech;
    //private boolean isRepeatEnabled = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        colorCodeTextView = findViewById(R.id.colorCodeTextView); // Initialize TextView
        colorCodeTextRGBView = findViewById(R.id.colorCodeTextRGBView); // Initialize TextView

        Button captureButton = findViewById(R.id.btnCapture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Set the language for TTS (you can choose a different language)
                    int result = textToSpeech.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //Log.e("TTS", "Language is not supported.");
                        System.out.println("TTS - Language is not supported.");
                    }
                } else {
                    //Log.e("TTS", "Initialization failed.");
                    System.out.println("TTS - Initialization failed.");
                }
            }
        });

        // Set click listener for the repeat button
        Button repeatButton = findViewById(R.id.btnRepeat);
        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakColorCode(colorCodeTextView.getText().toString());
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.mh.dominantcolor.provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Image captured, now analyze it for the dominant color
            analyzeDominantColor();
        }
    }

    private void analyzeDominantColor() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                int dominantColor = palette.getDominantColor(Color.BLACK);
                System.out.println("Dominant Color int: " +dominantColor);
                String hexCode = String.format("#%06X", (0xFFFFFF & dominantColor));
                System.out.println("Hexcode of Dominant color:" + hexCode);
                imageView.setBackgroundColor(dominantColor);

                // Display the color code as a hex string
                String colorCode = String.format("#%06X", (0xFFFFFF & dominantColor));
                colorCodeTextView.setText("Dominant Color Hex: " + colorCode);

                int r = Integer.valueOf(hexCode.substring(1, 3), 16);
                int g = Integer.valueOf(hexCode.substring(3, 5), 16);
                int b = Integer.valueOf(hexCode.substring(5, 7), 16);

                colorCodeTextRGBView.setText("Dominant Color RGB: " + r+", "+g+", "+b);

                // Show the "Repeat" button after displaying the color
                showRepeatButton();

                // Speak the color code
                speakColorCode(colorCode);
            }
        });
    }

    private void speakColorCode(String colorCode) {
        if (textToSpeech != null) {
            textToSpeech.speak("The dominant color code is " + colorCode, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showRepeatButton() {
        Button repeatButton = findViewById(R.id.btnRepeat);
        repeatButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        // Release the Text-to-Speech engine when your app is finished
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

}
