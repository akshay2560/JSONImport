package com.example.jsonimport;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jsonimport.Helper.FilePath;
import com.example.jsonimport.Models.ConfigData;
import com.example.jsonimport.Room.MyRoomDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Configuration extends AppCompatActivity {

    private static final int MY_PERMISSION=1111;
    private static final int READ_FILE_REQUEST_CODE = 42;
    private static final int CHOOSE_FILE_REQUEST_CODE = 8778;
    private static final String TAG=Configuration.class.getSimpleName();

    public static MyRoomDatabase myRoomDatabase;

    TextView acun,devicekey,filechosen;
    EditText networkkey,unicastmeshaddress,appkey,gga,acuga,hsga,ek,oid,bid,bdir;
    Button process,save;
    Intent intent;
    String jsonString;
    ConfigData configData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_data);

        checkForPermission();
        myRoomDatabase= Room.databaseBuilder(getApplicationContext(),MyRoomDatabase.class,"configdb").allowMainThreadQueries().build();
        acun=(TextView)findViewById(R.id.acun);
       networkkey=(EditText) findViewById(R.id.network_key);
        unicastmeshaddress=(EditText) findViewById(R.id.unicast);
        devicekey=(TextView) findViewById(R.id.device_key);

       appkey =(EditText) findViewById(R.id.app_key);
        gga=(EditText) findViewById(R.id.gga);
        acuga=(EditText) findViewById(R.id.acuga);
        hsga=(EditText) findViewById(R.id.hsga);
        ek=(EditText) findViewById(R.id.encryption_key);
        oid=(EditText) findViewById(R.id.organisation_id);
        bid=(EditText) findViewById(R.id.barrier_id);
        bdir=(EditText) findViewById(R.id.barrier_dir);
        filechosen=(TextView) findViewById(R.id.file_chosen);

        save=(Button)findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(configData==null){
                    Toast.makeText(Configuration.this,"Please Enter Data",Toast.LENGTH_SHORT).show();
                }else
                {
                    ConfigData savingData=new ConfigData(acun.getText().toString(),networkkey.getText().toString(),unicastmeshaddress.getText().toString(),appkey.getText().toString(),
                            gga.getText().toString(),acuga.getText().toString(),hsga.getText().toString(),ek.getText().toString(),oid.getText().toString(),bid.getText().toString(),bid.getText().toString());
                    try {

                        Configuration.myRoomDatabase.myDao().addConfigData(savingData);
                        Toast.makeText(Configuration.this,"Data Added Successfully",Toast.LENGTH_SHORT).show();
                        nullifyView();
                        jsonString=null;
                        filechosen.setText("");
                    }catch (Exception ex){
                        System.out.println("ex = " + ex);
                        Toast.makeText(Configuration.this, "There was a Problem adding your Data to the Database", Toast.LENGTH_SHORT).show();
                        jsonString=null;
                    }
                }
            }
        });



        process =(Button) findViewById(R.id.process_file);
        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(jsonString!=null) {
                    processJsonString(jsonString);
                }else
                {
                    Toast.makeText(Configuration.this,"Please Select a File",Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void nullifyView(){
        acun.setText("");networkkey.setText("");unicastmeshaddress.setText("");devicekey.setText("");
        appkey.setText("");gga.setText("");acuga.setText("");hsga.setText("");ek.setText("");
        oid.setText("");bid.setText("");bdir.setText("");
    }

    private void checkForPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){

            }else
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },MY_PERMISSION);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        Log.i(TAG, "onActivityResult: "+requestCode);
        Log.i(TAG, "intent.getData() "+intent.getData());
        Log.i(TAG, "intent.getData().getPath() "+intent.getData().getPath());

        String filename=intent.getData().getPath().substring(intent.getData().getPath().lastIndexOf("/")+1);
        filechosen.setText(filename);
        final Uri uri = intent.getData();
        jsonString = readJSONStringFromUri(uri);

    }

    /***
     * The Function processes the Uri and get the proper file path and reads the contents of the file and return them in a string
     * @param uri - Uri of the Selected path
     * @return - Json String
     */
    private String readJSONStringFromUri(Uri uri) {
        String stringJson="";
        if(uri.getPath()!=null){
            String selectedFilePath = FilePath.getPath(Configuration.this,uri);
            try {
                StringBuilder stringBuilder=new StringBuilder();
                final File file = new File(selectedFilePath);
                FileInputStream fileInputStream=new FileInputStream(file);
                InputStreamReader inputStreamReader=new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader=new BufferedReader(inputStreamReader);

                while((stringJson=bufferedReader.readLine())!=null){
                    stringBuilder.append(stringJson);
                }
                fileInputStream.close();
                stringJson=stringBuilder.toString();
                bufferedReader.close();
            }
            catch(Exception ex){
                System.out.println("ex = " + ex);
            }
        }
        return stringJson;
    }

    /***
     * Method to Process the output string after file reading
     * Json Parsing and storing in the Config class and at the same time setting the Views according to the data received
     * @param jsonString
     */
    private void processJsonString(String jsonString) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonString);
            Gson gson = new Gson();
            configData=gson.fromJson(jsonElement, ConfigData.class);
            acun.setText("-For-"+configData.getAccess_control_unit_name());
            networkkey.setText(configData.getNetworkKey());
            unicastmeshaddress.setText(configData.getUnicastMeshAddress());
            appkey.setText(configData.getAppKey());
            gga.setText(configData.getGga());
            acuga.setText(configData.getAcuga());
            hsga.setText(configData.getHsga());
            ek.setText(configData.getEncryptionKey());
            oid.setText(configData.getOrganisationId());
            bid.setText(configData.getBarrierId());
            bdir.setText(configData.getBarrierDir());
        }
        catch(Exception ex){
            System.out.println("ex = " + ex);
            Toast.makeText(Configuration.this,"Sorry Dude !!!! Wrong Json File",Toast.LENGTH_SHORT).show();

        }
    }


    public void chooseFile(View view) {
        System.out.println("Button Clicked");
        if(Utils.isKitkatOrAbove()){
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent,READ_FILE_REQUEST_CODE);

    }

}

