package com.example.jsonimport.Models;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "configData")
public class ConfigData implements Serializable {

    @SerializedName("access_control_unit_name")
    private String access_control_unit_name;
    @SerializedName("network_key")
    private String networkKey;
    @SerializedName("unicast_mesh_address")
    private String unicastMeshAddress;
    @SerializedName("device_key")
    private String deviceKey;
    @SerializedName("app_key")
    private String appKey;
    @SerializedName("access_control_unit_group_address")
    private String acuga;
    @SerializedName("gateway_group_address")
    private String gga;
    @SerializedName("health_server_group_address")
    private String hsga;
    @SerializedName("encryption_key")
    private String encryptionKey;
    @SerializedName("organisation_id")
    private String organisationId;
    @SerializedName("barrier_id")
    private String barrierId ;
    @SerializedName("barrier_dir")
    private String barrierDir;
    @PrimaryKey
    @NonNull
    @SerializedName("mac_id")
    private String macId;


    public ConfigData(String access_control_unit_name,String networkKey,String unicastMeshAddress,String appKey,String gga,String acuga,String hsga,String encryptionKey,String organisationId,String barrierId,String barrierDir,String macId){
        this.access_control_unit_name=access_control_unit_name;
        this.networkKey=networkKey;
        this.unicastMeshAddress=unicastMeshAddress;
        this.appKey=appKey;
        this.gga=gga;
        this.acuga=acuga;
        this.hsga=hsga;
        this.encryptionKey=encryptionKey;
        this.organisationId=organisationId;
        this.barrierId=barrierId;
        this.barrierDir=barrierDir;
        this.macId = macId;
    }

    //Getter methods
    public String getAccess_control_unit_name() {
        return access_control_unit_name;
    }

    public String getNetworkKey() {
        return networkKey;
    }

    public String getUnicastMeshAddress() {
        return unicastMeshAddress;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAcuga() {
        return acuga;
    }

    public String getGga() {
        return gga;
    }

    public String getHsga() {
        return hsga;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public String getBarrierId() {
        return barrierId;
    }

    public String getBarrierDir() {
        return barrierDir;
    }

    public String getMacId(){return macId;}

    //Setter methods
    public void setAccess_control_unit_name(String access_control_unit_name) {
        this.access_control_unit_name = access_control_unit_name;
    }

    public void setNetworkKey(String networkKey) {
        this.networkKey = networkKey;
    }

    public void setUnicastMeshAddress(String unicastMeshAddress) {
        this.unicastMeshAddress = unicastMeshAddress;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setAcuga(String acuga) {
        this.acuga = acuga;
    }

    public void setGga(String gga) {
        this.gga = gga;
    }

    public void setHsga(String hsga) {
        this.hsga = hsga;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public void setBarrierId(String barrierId) {
        this.barrierId = barrierId;
    }

    public void setBarrierDir(String barrierDir) {
        this.barrierDir = barrierDir;
    }

    public void setMacId(String macId){this.macId = macId;}

}
