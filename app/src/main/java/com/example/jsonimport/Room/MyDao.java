package com.example.jsonimport.Room;

import androidx.room.Dao;
import androidx.room.Insert;

import com.example.jsonimport.Models.ConfigData;

@Dao
public interface MyDao {

    @Insert
    public void addConfigData(ConfigData configData);

}
