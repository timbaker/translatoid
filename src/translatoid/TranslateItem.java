package translatoid;

import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TranslateItem implements Serializable {
    private SimpleStringProperty key;
    private SimpleStringProperty value;
    private KeyStatus status = KeyStatus.NO_CHANGE;
    private boolean itemChanged = false;
    private boolean ignore = false;
    private String fileName;

    public TranslateItem(String key, String value) {
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
        status = LanguageManager.checkKey(key);
    }

    public TranslateItem(String key, String value, String file) {
        this.fileName = file;
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
        this.status = LanguageManager.checkKey(key);
    }

    public TranslateItem(String key, String value, KeyStatus keyStatus) {
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
        this.status = keyStatus;
    }

    public String getKey() {
        return key.get();
    }

    public String getValue() {
        return value.get();
    }
    public void setValue(String v) {
        value.setValue(v);
    }

    public KeyStatus getStatus() {
        return status;
    }

    public void setStatus(KeyStatus s) {
        status = s;
    }

    public String getFileName() {
        return fileName;
    }

    public void markEdited() {
        itemChanged = true;
    }

    public void reset() {
        itemChanged = false;
    }

    public boolean isEdited() {
        return itemChanged;
    }

    public void markIgnore(boolean i) {
        ignore = i;
    }

    public boolean isIgnore() {
        return ignore;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(key.get());
        s.writeObject(value.get());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        key = new SimpleStringProperty((String) s.readObject());
        value = new SimpleStringProperty((String) s.readObject());
    }
}
