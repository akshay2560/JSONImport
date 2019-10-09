package com.example.jsonimport.Activities;

import androidx.annotation.Nullable;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jsonimport.BleApi.ControlApi;
import com.example.jsonimport.Helper.FilePath;
import com.example.jsonimport.Models.ConfigData;
import com.example.jsonimport.R;
import com.example.jsonimport.Room.MyRoomDatabase;
import com.example.jsonimport.Shared.Config;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import no.nordicsemi.android.meshprovisioner.AllocatedUnicastRange;
import no.nordicsemi.android.meshprovisioner.MeshManagerApi;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.Provisioner;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Configuration extends AppCompatActivity {

    private static final int MY_PERMISSION=1111;
    private static final int READ_FILE_REQUEST_CODE = 42;
    private static final int CHOOSE_FILE_REQUEST_CODE = 8778;
    private static final String TAG=Configuration.class.getSimpleName();

    public static MyRoomDatabase myRoomDatabase;

    TextView acun,devicekey,filechosen;
    EditText networkkey,unicastmeshaddress,appkey,gga,acuga,hsga,ek,oid,bid,bdir,macId;
    Button process,save;
    Intent intent;
    String jsonString;
    ConfigData configData;
    MeshManagerApi meshManagerApi;
    String macID;
    ControlApi controlApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_data);

        controlApi = ControlApi.getInstance(this);
        meshManagerApi = MeshManagerApi.getInstance(this);

        checkForPermission();
        myRoomDatabase= MyRoomDatabase.getDatabase(this);
        acun= findViewById(R.id.acun);
        networkkey= findViewById(R.id.network_key);
        unicastmeshaddress= findViewById(R.id.unicast);
        devicekey= findViewById(R.id.device_key);
        macId = findViewById(R.id.macId);

        appkey = findViewById(R.id.app_key);
        gga= findViewById(R.id.gga);
        acuga= findViewById(R.id.acuga);
        hsga= findViewById(R.id.hsga);
        ek= findViewById(R.id.encryption_key);
        oid= findViewById(R.id.organisation_id);
        bid= findViewById(R.id.barrier_id);
        bdir= findViewById(R.id.barrier_dir);
        filechosen= findViewById(R.id.file_chosen);

        save= findViewById(R.id.save);
        save.setOnClickListener(view -> {
            if(configData==null){
                Toast.makeText(Configuration.this,"Please Enter Data",Toast.LENGTH_SHORT).show();
            }else if(bid.length() < 4) {
                Toast.makeText(getApplicationContext(),"Enter 2 Bytes of Barrier Id",Toast.LENGTH_SHORT).show();
            }else if(bdir.length() > 2 ) {
                Toast.makeText(getApplicationContext(),"Enter 1 Byte of Barrier Direction",Toast.LENGTH_SHORT).show();
            }else{
                byte[] barrID = hexToByteArray(bid.getText().toString());
                byte temp = barrID[1];
                barrID[1] = barrID[0];
                barrID[0] = temp;
                String barrid = MeshParserUtils.bytesToHex(barrID,false);
                macID = macId.getText().toString();
                Log.e("BarrId", barrid);
                Log.e("BarrDir", bdir.getText().toString());
                Log.e("Barrier Id", MeshParserUtils.bytesToHex(barrID,false));
                ConfigData savingData=new ConfigData(acun.getText().toString(),networkkey.getText().toString(),unicastmeshaddress.getText().toString(),appkey.getText().toString(),
                        gga.getText().toString(),acuga.getText().toString(),hsga.getText().toString(),ek.getText().toString(),oid.getText().toString(),barrid,bdir.getText().toString(),macId.getText().toString());
                try {
                    MeshNetwork meshNetwork = new MeshNetwork("8DCDCE34-D3D8-4D30-A70E-6E32B968FDFF");
                    meshNetwork.setUnicastAddress(Integer.parseInt(unicastmeshaddress.getText().toString(),16));
                    meshNetwork.addNetKey(0,networkkey.getText().toString());
                    meshNetwork.addAppKey(0,appkey.getText().toString());
                    String provisionerUuid = UUID.randomUUID().toString().toUpperCase(Locale.US);
                    AllocatedUnicastRange allocatedUnicastRange = new AllocatedUnicastRange(0x0001,0x7FFF);
                    final List<AllocatedUnicastRange> ranges = new ArrayList<>();
                    ranges.add(allocatedUnicastRange);
                    final Provisioner provisioner = new Provisioner(provisionerUuid, ranges, null, null, "8DCDCE34-D3D8-4D30-A70E-6E32B968FDFF");
                    provisioner.setLastSelected(true);
                    final List<Provisioner> provisioners = new ArrayList<>();
                    provisioners.add(provisioner);
                    meshNetwork.setProvisioners(provisioners);
                    meshManagerApi.insertNetwork(meshNetwork);
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
        });


        process = findViewById(R.id.process_file);
        process.setOnClickListener(view -> {
            if(jsonString!=null) {
                processJsonString(jsonString);
            }else
            {
                Toast.makeText(Configuration.this,"Please Select a File",Toast.LENGTH_SHORT).show();
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
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.selectDoor){
            Intent intent = new Intent(this,Scanner.class);
            intent.putExtra("macID", macID);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ControlApi.isLocationPermissionsGranted(this)) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle(getApplicationContext().getString(R.string.app_name))
                    .setMessage("Location permission is required for BLE. Please turn it on")
                    .setPositiveButton("Ok", null)
                    .setOnDismissListener(dialogInterface -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE}, Config.PERMISSION_REQUEST);
                        } else {
                            Intent locationPermissionIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            locationPermissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            locationPermissionIntent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(locationPermissionIntent);
                        }
                    }).show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTypeface(Config.typefaceMedium);
        } else if (!ControlApi.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else if (!ControlApi.isLocationEnabled(this)) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setMessage("Location (Network) is required to be turned on for Bluetooth LE")
                    .setPositiveButton("Turn On", (dialogInterface, i) -> {
                        Intent locationEnableIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(locationEnableIntent);
                    }).show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTypeface(Config.typefaceMedium);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        Log.i(TAG, "onActivityResult: "+requestCode);
//        Log.i(TAG, "intent.getData() "+intent.getData());
//        Log.i(TAG, "intent.getData().getPath() "+intent.getData().getPath());
        if(intent ==null) {
            Toast.makeText(getApplicationContext(), "Please Choose a file", Toast.LENGTH_SHORT).show();
        } else if(intent.getData()==null) {
                Toast.makeText(getApplicationContext(),"Please Choose a file",Toast.LENGTH_SHORT).show();
        } else {
            String filename = intent.getData().getPath().substring(intent.getData().getPath().lastIndexOf("/") + 1);
            filechosen.setText(filename);
            final Uri uri = intent.getData();
            jsonString = readJSONStringFromUri(uri);
        }

    }

    /***
     * The Function processes the Uri and get the proper file path and reads the contents of the file and return them in a string
     * @param uri - Uri of the Selected path
     * @return - Json String
     */
    private String readJSONStringFromUri(Uri uri) {
        String stringJson="";
        if(uri.getPath()!=null) {
            String selectedFilePath = null;
            try {

            selectedFilePath = FilePath.getPath(Configuration.this, uri);

                Log.e(TAG, "selectedFilePath "+selectedFilePath);
 //               selectedFilePath= Commons.getPath(uri,this);
            } catch (Exception ex) {
                Toast.makeText(this,"Please Select File From Phone Storage",Toast.LENGTH_SHORT);
                Log.e(TAG, "FilePath parsing Problem : " + ex.getMessage());
            }


            if (selectedFilePath != null) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    final File file = new File(selectedFilePath);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    while ((stringJson = bufferedReader.readLine()) != null) {
                        stringBuilder.append(stringJson);
                    }
                    fileInputStream.close();
                    stringJson = stringBuilder.toString();
                    bufferedReader.close();
                    Log.i("","VALID FILE PATH : "+selectedFilePath);
                } catch (Exception ex) {
                    Log.e("", "Tried Reading File with given Filepath = " + ex);
                    Toast.makeText(this, "Invalid File Path", Toast.LENGTH_SHORT).show();
                }
            }else{
                Log.e("","Selected File Path is NULL");

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
                configData = gson.fromJson(jsonElement, ConfigData.class);
                acun.setText("-For-" + configData.getAccess_control_unit_name());
                networkkey.setText(configData.getNetworkKey());
                unicastmeshaddress.setText(configData.getUnicastMeshAddress());
                macId.setText(configData.getMacId());
                appkey.setText(configData.getAppKey());
                gga.setText(configData.getGga());
                acuga.setText(configData.getAcuga());
                hsga.setText(configData.getHsga());
                ek.setText(configData.getEncryptionKey());
                oid.setText(configData.getOrganisationId());
                bid.setText(configData.getBarrierId());
                bdir.setText(configData.getBarrierDir());

            } catch (Exception ex) {
                System.out.println("ex = " + ex);
                Toast.makeText(Configuration.this, "Incorrect Json File", Toast.LENGTH_SHORT).show();
            }
    }


    public void chooseFile(View view) {
//        System.out.println("Button Clicked");
//             if(FilePath.isKitkatOrAbove()){
//            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        } else {
//            intent = new Intent(Intent.ACTION_GET_CONTENT);
//        }
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
//        startActivityForResult(intent,READ_FILE_REQUEST_CODE);

      intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
         intent.setType("*/*");
     //  startActivityForResult(Intent.createChooser(intent,"Select Files To Upload"),READ_FILE_REQUEST_CODE);
        startActivityForResult(intent,READ_FILE_REQUEST_CODE);

     }

    public static byte[] hexToByteArray(String hex) {
        hex = hex.length() % 2 != 0 ? "0" + hex : hex;

        byte[] b = new byte[hex.length() / 2];

        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }



}

