package translatoid;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;

public class PopUpEditor extends Stage {
    private static PopUpEditor _instance;
    
    @FXML
    public TextArea engTextArea;
    
    @FXML
    public TextArea translatedTextArea;

    @FXML
    private Button editorApply;

    @FXML
    private Button editorPrevious;

    @FXML
    private Button editorNext;

    @FXML
    private Button editorCancel;
    
    private TableView<TranslateItem> m_tableView;
    private TranslateItem m_translateItem;

    public static PopUpEditor instance() throws IOException {
        if (_instance == null) {
            FXMLLoader loader = new FXMLLoader(PopUpEditor.class.getResource("popUpEditor.fxml"));
            Parent root = loader.load();
            _instance = loader.getController();
            _instance.setScene(new Scene(root));
            _instance.setTitle("String Editor");
            _instance.create();
        }
        return _instance;
    }

    public PopUpEditor() {
    }
    
    public void setTranslateItem(TableView<TranslateItem> tableView, int index) {
        m_tableView = tableView;
        m_translateItem = tableView.getItems().get(index);
        String english = LanguageManager.getCurrentEnglishString(m_translateItem.getKey());
        setStrings(english, m_translateItem.getValue());
        
        m_tableView.getSelectionModel().clearAndSelect(index);
//        m_tableView.scrollTo(index);
        
        editorPrevious.setDisable(index == 0);
        editorNext.setDisable(index == m_tableView.getItems().size() - 1);
        
        translatedTextArea.requestFocus();
        translatedTextArea.positionCaret(translatedTextArea.getLength());
        
        setTitle("String Editor - " + m_translateItem.getKey());
    }
    
    public void setStrings(String english, String translation) {
        engTextArea.setText(english);
        translatedTextArea.setText(translation);
    }

    private void create() {
        editorApply.setOnAction(event -> this.apply());
        editorCancel.setOnAction(event -> this.cancel());
        editorPrevious.setOnAction(event -> this.previousString());
        editorNext.setOnAction(event -> this.nextString());
    }

    private void apply() {
        String translation = translatedTextArea.getText();
        if (translation.equals(m_translateItem.getValue())) {
            return;
        }
        m_translateItem.setValue(translation);
        m_translateItem.markEdited();
        LanguageManager.unsavedChanges = true;
        m_tableView.refresh();
    }
    
    private void cancel() {
        hide();
    }

    private void previousString() {
        int index = m_tableView.getItems().indexOf(m_translateItem);
        if (index > 0) {
            setTranslateItem(m_tableView, index - 1);
        }
    }
    
    private void nextString() {
        int index = m_tableView.getItems().indexOf(m_translateItem);
        if (index < m_tableView.getItems().size() - 1) {
            setTranslateItem(m_tableView, index + 1);
        }
    }
}
