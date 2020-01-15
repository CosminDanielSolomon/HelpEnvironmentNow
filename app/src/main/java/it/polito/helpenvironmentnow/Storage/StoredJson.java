package it.polito.helpenvironmentnow.Storage;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class StoredJson {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String type;
    public String jsonSave;
}
