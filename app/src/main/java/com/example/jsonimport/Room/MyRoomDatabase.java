package com.example.jsonimport.Room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.jsonimport.Models.ConfigData;

@Database(entities={ConfigData.class},version = 1)
public abstract class MyRoomDatabase extends RoomDatabase {

    public abstract MyDao myDao();

}
