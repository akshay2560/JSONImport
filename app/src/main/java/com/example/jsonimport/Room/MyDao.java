package com.example.jsonimport.Room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;


import com.example.jsonimport.Models.ConfigData;

@Dao
public interface MyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addConfigData(ConfigData configData);

    @Query("SELECT networkKey from configData WHERE macId == :macID")
    String getNetworkKey(String macID);

    @Query("SELECT appKey from configData WHERE macId == :macID")
    String getAppKey(String macID);

    @Query("SELECT unicastMeshAddress from configData WHERE macId == :macID")
    String getUnicastAddress(String macID);

    @Query("SELECT acuga from configData WHERE macId == :macID")
    String getAccessControlUnitGroupAddress(String macID);

    @Query("SELECT gga from configData WHERE macId == :macID")
    String getGatewayGroupAddress(String macID);

    @Query("SELECT hsga from configData WHERE macId == :macID")
    String getHealthServerGroupAddress(String macID);

    @Query("SELECT encryptionKey from configData WHERE macId == :macID")
    String getEncryptionKey(String macID);

    @Query("SELECT organisationId from configData WHERE macId == :macID")
    String getOrgId(String macID);

    @Query("SELECT barrierId from configData WHERE macId == :macID")
    String getBarrierId(String macID);

    @Query("SELECT barrierDir from configData WHERE macId == :macID ")
    String getBarrierDirection(String macID);
}
