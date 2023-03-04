package com.example.facedetection2023_it;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnSuccessListener<Text>, OnFailureListener {
    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    public Bitmap mSelectedImage;
    public ImageView mImageView;
    public TextView txtResults;

    public Button btCamera, btGaleria;
    ArrayList<String> permisosNoAprobados;
    ObjectDetector objectDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);
        txtResults.setMovementMethod(new ScrollingMovementMethod());
        btCamera = findViewById(R.id.btCamera);
        btGaleria = findViewById(R.id.btGallery);
        //txtResults.setText("Etiqueta "+"\n"+"Objeto: "+"\n"+"Etiqueta "+"\n"+"Objeto: "+"\n");

        ArrayList<String> permisos_requeridos = new ArrayList<String>();
        permisos_requeridos.add(Manifest.permission.CAMERA);
        permisos_requeridos.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        permisos_requeridos.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        permisosNoAprobados = getPermisosNoAprobados(permisos_requeridos);

        requestPermissions(permisosNoAprobados.toArray(new String[permisosNoAprobados.size()]),
                100);
        //Detección de objetos
        // Multiple object detection in static images
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();
        objectDetector = ObjectDetection.getClient(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.CAMERA)) {
                btCamera.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            } else if (permissions[i].equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ||
                    permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                btGaleria.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }

    public ArrayList<String> getPermisosNoAprobados(ArrayList<String> listaPermisos) {
        ArrayList<String> list = new ArrayList<String>();
        Boolean habilitado;
        if (Build.VERSION.SDK_INT >= 23)
            for (String permiso : listaPermisos) {
                if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                    list.add(permiso);
                    habilitado = false;
                } else
                    habilitado = true;

                if (permiso.equals(Manifest.permission.CAMERA))
                    btCamera.setEnabled(habilitado);
                else if (permiso.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ||
                        permiso.equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                    btGaleria.setEnabled(habilitado);
            }
        return list;
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void OCRfx(View v) {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void objetos(View v) {
        mImageView.setDrawingCacheEnabled(true);
        mImageView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(mImageView.getDrawingCache());

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        /*if (mSelectedImage == null){
            System.out.println("BITMAP IS NULL");
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);*/
        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(@NonNull List<DetectedObject> detectedObjects) {
                                System.out.println("eN onSuccess");
                                if (!detectedObjects.isEmpty()) {
                                    int contador = 0;
                                    String respuestas="";
                                    for (DetectedObject detectedObject : detectedObjects) {
                                        Rect boundingBox = detectedObject.getBoundingBox();
                                        Integer trackingId = detectedObject.getTrackingId();
                                        System.out.println("boundingBox: " + boundingBox + " trackingId: " + trackingId);

                                        Bitmap originalBitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
                                        Bitmap copiaBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                        Canvas canvas = new Canvas(copiaBitmap);
                                        Paint paint = new Paint();
                                        paint.setColor(Color.RED);
                                        paint.setStyle(Paint.Style.STROKE);
                                        paint.setStrokeWidth(5);
                                        canvas.drawRect(boundingBox, paint);
                                        mImageView.setImageBitmap(copiaBitmap);

                                        TextPaint textPaint = new TextPaint();
                                        textPaint.setColor(Color.RED);
                                        textPaint.setTextSize(30);


                                        for (DetectedObject.Label label : detectedObject.getLabels()) {
                                            // objeto
                                            String text = label.getText();
                                            System.out.println("Tipo de objeto: " + text);
                                            // etiqueta
                                            int index = label.getIndex();//esto es una constante del tipo de objeto al que pertenece
                                            System.out.println("Número de índice: " + contador);
                                            // confianza
                                            float confidence = label.getConfidence();
                                            System.out.println("Confianza: " + confidence);
                                            canvas.drawText(text+", "+confidence, boundingBox.left, boundingBox.top - 20, textPaint);
                                            respuestas= respuestas+"Etiqueta "+contador+"\n"+"Objeto: "+text+"\n"+"Confianza: "+confidence+"\n";
                                        }
                                        contador= contador+1;
                                    }
                                    txtResults.setText(respuestas);
                                } else {
                                    System.out.println("No se cargo la imagen F");
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                System.out.println("ERROR: " + e.getMessage());
                            }
                        });
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados = "";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        } else {
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados = resultados + elements.get(k).getText() + " ";
                    }
                }
                resultados = resultados + "\n";
            }
        }
        txtResults.setText(resultados);
    }
}