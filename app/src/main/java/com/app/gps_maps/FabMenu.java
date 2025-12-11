package com.app.gps_maps;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.views.MapView;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class FabMenu{
    private FloatingActionButton fabMenuBtn;
    private Map<Integer, FloatingActionButton> subBtns = new HashMap<>();
    private Map<Integer, LinearLayout> layouts = new HashMap<>();
    private boolean isOpen = false;
    private double longitude, latitude;
    private boolean isSmsPermissionGranted;
    private final Context context;
    private MapView osm;

    public FabMenu(Context context, FloatingActionButton menuBtn, FloatingActionButton[] subBtns, LinearLayout[] layouts) {
        this.context = context;
        this.fabMenuBtn = menuBtn;
        for (int i=1;i<=subBtns.length; i++){
            this.subBtns.put(i, subBtns[i-1]);
            Log.d("asdasd", String.valueOf(this.subBtns.get(i)));
        }
        for (int i=1;i<=layouts.length; i++){
            this.layouts.put(i, layouts[i-1]);
            Log.d("asdasd", String.valueOf(this.layouts.get(i)));
        }

//        String[] titles = {"Wyslij smsa", "zapisz koordynaty", "udostepnij", "pogoda"};
        Log.d("asdasd", String.valueOf(this.fabMenuBtn ));
        this.fabMenuBtn.setOnClickListener(v->{
//            Toast.makeText(context, "aksjdnajksndjashndjasd", Toast.LENGTH_SHORT).show();
            if(isOpen)closeMenu();
            else openMenu();
        });

        //fab options
        //send sms
        this.subBtns.get(1).setOnClickListener(v->{
            alertDialogSmsSend();
        });
        //save
        this.subBtns.get(2).setOnClickListener(v->{
            captureOsmToGallery();
        });
        //share
        this.subBtns.get(3).setOnClickListener(v->{
            shareCordinatesIntent();
        });
        //pogoda
        this.subBtns.get(4).setOnClickListener(v->{
            Intent intent = new Intent(context, WeatherActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            context.startActivity(intent);
        });
    }

    private void openMenu() {
        this.layouts.forEach((k, v)->{
            Log.e("asasd", String.valueOf(k+" "+v));
            v.setVisibility(View.VISIBLE);
            v.setAlpha(0f);
            v.animate().translationY(-120).alpha(1f).setDuration(200).start();
        });

        ObjectAnimator rotate = ObjectAnimator.ofFloat(this.fabMenuBtn, "rotation", 0f, 45f);
        rotate.setDuration(200);
        rotate.start();

        isOpen = true;
    }

    private void closeMenu() {
        this.layouts.forEach((k, v)->{
            v.animate().translationY(0).alpha(0f).setDuration(200).withEndAction(()->
                    v.setVisibility(View.GONE)).start();
        });

        ObjectAnimator rotate = ObjectAnimator.ofFloat(this.fabMenuBtn, "rotation", 45f, 0f);
        rotate.setDuration(200);
        rotate.start();

        isOpen = false;
    }

    public void setMeasurementsInFabMenuToShare(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }
    public void setSmsPermissionGranted(boolean isSmsPermissionGranted){
        this.isSmsPermissionGranted = isSmsPermissionGranted;
    }
    public void setOsm(MapView osm){
        this.osm = osm;
    }
    @SuppressLint("ResourceAsColor")
    private void alertDialogSmsSend(){
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(context);
        textView.setText("Podaj numer telefonu: ");
        textView.setTextColor(Color.WHITE);

        EditText editText = new EditText(context);
        editText.setHint("nr. telefonu");
        editText.setHintTextColor(Color.WHITE);

        linearLayout.addView(textView);
        linearLayout.addView(editText);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme)
                .setView(linearLayout)
//                    .setTitle("Wyslij sms z kordynatami na podany numer")
                .setCancelable(true)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Wyslij", (dialog, which) -> {
                    if(!editText.getText().toString().isEmpty()){
                        if(this.isSmsPermissionGranted){
                            String destinationAddress = editText.getText().toString();
                            String text = this.longitude+" "+this.latitude;
                            if(!destinationAddress.equals("") && !text.equals(" ")){
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(
                                        destinationAddress,
                                        null,
                                        text,
                                        null,
                                        null
                                );
                                Toast.makeText(context, "SMS send", Toast.LENGTH_SHORT).show();
                                Log.v("SMS", "Sms send");
                                AlertDialog.Builder information = new AlertDialog.Builder(context, R.style.DialogTheme)
                                        .setMessage("Wylsano pomyslnie sms na numer "+destinationAddress+ " z kordynatami "+text)
                                        .setCancelable(true)
                                        .setNeutralButton("OK", (dialog1, which1) -> {dialog1.cancel();});
                                information.create().show();
                            }
                        }else{
                            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show();
                            Log.v("SMS", "Permission denied");
                            AlertDialog.Builder information = new AlertDialog.Builder(context, R.style.DialogTheme)
                                    .setMessage("blad przy wysylaniu sms")
                                    .setCancelable(true)
                                    .setNeutralButton("OK", (dialog1, which1) -> {dialog1.cancel();});
                            information.create().show();
                        }
                    }else{
                        Toast.makeText(context, "podaj numer telefonu", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.create().show();
    }
    private void captureOsmToGallery(){
        try{
            Bitmap bitmap = Bitmap.createBitmap(this.osm.getMeasuredWidth(),
                    this.osm.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            this.osm.draw(canvas);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
            String file_name = "map_screnshot_"+LocalDateTime.now().format(formatter);

//                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), file_name+".png");
            MediaStore.Images.Media.insertImage(this.context.getContentResolver(), bitmap, file_name, null);


            AlertDialog.Builder information = new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("zapisano zdjecie mapy w "+Environment.DIRECTORY_PICTURES)
                    .setCancelable(true)
                    .setNeutralButton("OK", (dialog1, which1) -> {dialog1.cancel();});
            information.create().show();

        }catch (Exception e){
            e.printStackTrace();
            AlertDialog.Builder information = new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("blad przy zapisywaniu zdjecia do galeri")
                    .setCancelable(true)
                    .setNeutralButton("OK", (dialog1, which1) -> {dialog1.cancel();});
            information.create().show();
        }
    }
    private void shareCordinatesIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, this.longitude+" "+this.latitude);
        intent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(intent, null);
        this.context.startActivity(shareIntent);
    }

}
