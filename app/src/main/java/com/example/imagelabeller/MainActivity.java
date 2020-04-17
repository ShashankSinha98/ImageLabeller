package com.example.imagelabeller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private ImageView imageView;
    private Bitmap bitmap;
    private FirebaseVisionImage firebaseVisionImage;
    private FirebaseVisionLabelDetector labelDetector;
    private TextView resultDisplayTV;
    private Button chooseImage, scanImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Getting ID's
        imageView = findViewById(R.id.text_image);
        resultDisplayTV = findViewById(R.id.results_tv);
        chooseImage = findViewById(R.id.add_img_btn);
        scanImage = findViewById(R.id.scan_image_btn);


        // Choose Img
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    } else {

                        bringImagePicker();
                    }
                } else {

                    bringImagePicker();
                }
            }
        });


        // Search Code
        scanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultDisplayTV.setText("");

                // Getting bitmap from Imageview
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                bitmap = drawable.getBitmap();

                runImageLabelRecoginition();
                
            }
        });

    }

    private void runImageLabelRecoginition() {

        firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        labelDetector = FirebaseVision.getInstance().getVisionLabelDetector();

        labelDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionLabel> firebaseVisionLabels) {
                        chooseImage.setEnabled(true);
                        scanImage.setEnabled(true);

                        processImageLabelResult(firebaseVisionLabels);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                chooseImage.setEnabled(false);
                scanImage.setEnabled(false);
                resultDisplayTV.setText("Error : "+e.getMessage());
                Log.e(TAG+" Error: ",e.toString());
            }
        });


    }

    private void processImageLabelResult(List<FirebaseVisionLabel> firebaseVisionLabels) {

        if(firebaseVisionLabels.size()==0){
            resultDisplayTV.setText("Found Nothing");
        } else {
            for (FirebaseVisionLabel label: firebaseVisionLabels) {
                String text = label.getLabel();
                String entityId = label.getEntityId();
                float confidence = label.getConfidence();

                resultDisplayTV.setText(resultDisplayTV.getText()+"\nText-"+text+" :ID- "+entityId+" :Confidence- "+String.valueOf(confidence));
            }
        }
    }

    private void bringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1)
                .setCropShape(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? CropImageView.CropShape.RECTANGLE : CropImageView.CropShape.OVAL)
                .start(MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                resultDisplayTV.setText("");
                imageView.setImageURI(result.getUri());


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                resultDisplayTV.setText("Error Loading Image: "+error);

            }
        }
    }
}
