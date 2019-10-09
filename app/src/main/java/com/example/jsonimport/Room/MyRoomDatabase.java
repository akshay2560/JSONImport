package com.example.jsonimport.Room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;


import com.example.jsonimport.Models.ConfigData;

@Database(entities={ConfigData.class},version = 1)
public abstract class MyRoomDatabase extends RoomDatabase {

    public abstract MyDao myDao();

    private static volatile MyRoomDatabase INSTANCE;

    public static MyRoomDatabase getDatabase(Context context){
        if(INSTANCE == null){
            synchronized (MyRoomDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MyRoomDatabase.class,"config.db")
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
