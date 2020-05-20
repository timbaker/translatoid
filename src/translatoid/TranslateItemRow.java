package translatoid;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;

public class TranslateItemRow extends TableRow<TranslateItem> {
    public TranslateItemRow() {

    }

    @Override
    public void updateItem(TranslateItem item, boolean empty) {
        super.updateItem(item, empty) ;
        Tooltip tip = new Tooltip();
        String currentString;
        String previousString;
        String tipText = "";
        if (item != null) {
            previousString = LanguageManager.getPreviousEnglishString(item.getKey());
            currentString = LanguageManager.getCurrentEnglishString(item.getKey());
            tipText = "Current English Value: " + (currentString != null ? currentString : "NONE") + "\nPrevious English String: " + (previousString != null ? previousString : "NONE");
        }
        if (item == null) {
            setStyle("");
        } else if (item.getStatus() == KeyStatus.REMOVED) {
            System.out.println("REMOVED: " + item.getKey());
            setStyle("-fx-background-color: red;");
        } else if (item.getStatus() == KeyStatus.MODIFIED) {
            setStyle("-fx-background-color: orange");
        } else if (item.getStatus() == KeyStatus.ADDED) {
            setStyle("-fx-background-color: lawngreen");
        } else {
            setStyle("");
        }

        if (!tipText.isEmpty()) {
            tip.setText(tipText);
            setTooltip(tip);
        }


    }


}
