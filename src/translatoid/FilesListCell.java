package translatoid;

import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

public class FilesListCell extends ListCell<File> {
    @Override
    public void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        setText((empty || item == null) ? "" : item.getName());
    }
}
