package translatoid;

import java.io.Serializable;

public enum KeyStatus implements Serializable {
    NO_CHANGE,
    MODIFIED,
    REMOVED,
    ADDED
}
