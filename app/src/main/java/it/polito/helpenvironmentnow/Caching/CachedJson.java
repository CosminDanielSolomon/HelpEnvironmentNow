package it.polito.helpenvironmentnow.Caching;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class CachedJson {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String jsonSave;
}
