package translatoid;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Filter;


public class Controller {
    public enum FilterMode {
        KEY, VALUE, NONE
    }

    @FXML
    private Pane mainPane;

    @FXML
    private VBox mainVBox;
    @FXML
    private MenuItem menuOpenLanguage;
    @FXML
    private MenuItem itemEnglishToggle;

    //Item File Conversion Tab
    @FXML
    private ListView itemConversionListView;
    @FXML
    private Button itemConversionAddFile;
    ObservableList<File> items = FXCollections.observableArrayList();
    @FXML
    private Button itemConversionConvert;

    @FXML
    private ListView<File> translationFolderLanguages;
    @FXML
    private Label usernameLabel;
    @FXML
    private TreeView<File> fileTreeView;
    @FXML
    private TableView translationTable;
    @FXML
    private TableColumn<TranslateItem, String> translateKey;
    @FXML
    private TableColumn<TranslateItem, String> translateValue;
    @FXML
    private TableColumn<TranslateItem, String> translateEnglish;
    @FXML
    private MenuItem generateTranslationReport;
    @FXML
    private ChoiceBox filterDropdown;
    @FXML
    private TextField filterText;

    private TreeItem<File> rootItem;
    private Map<TreeItem<File>, ObservableList<TranslateItem>> fileMap;
    private Charset currentCharset = Charset.forName("Cp1252");
    private ContextMenu cm = new ContextMenu();
    private MenuItem resolveString = new MenuItem("Mark as Resolved");
    private MenuItem removeString = new MenuItem("Remove String");
    private MenuItem keepString = new MenuItem("Keep String");
    //private MenuItem openEditor = new MenuItem("Open String Editor");
    private MenuItem addString = new MenuItem();
    private TreeItem<File> currentFile;
    private File currentFolder;
    private Stage currentStage;
    private Alert alert;
    private String addIcon = "/icons/add.png";
    private String editIcon = "/icons/page_edit.png";
    private String deleteIcon = "/icons/delete.png";
    private FilterMode filterMode = FilterMode.NONE;
    private ObservableList<TranslateItem> originalList;

    ItemConverter converter = new ItemConverter();

    public void initialize() {
        LanguageManager.initCharsets();
        itemConversionListView.setItems(items);
        fileMap = new HashMap<>();
        fileTreeView.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {

            public TreeCell<File> call(TreeView<File> tv) {
                return new TreeCell<File>() {

                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((empty || item == null) ? "" : item.getName());
                        setGraphic(null);
                        if (fileMap.containsKey(getTreeItem())) {
                            for (TranslateItem translate : fileMap.get(getTreeItem())) {
                                if (translate.getStatus() == KeyStatus.ADDED) {
                                    setGraphic(new ImageView(new Image(getClass().getResourceAsStream(addIcon))));
                                    break;
                                }
                                else if (translate.getStatus() == KeyStatus.MODIFIED) {
                                    setGraphic(new ImageView(new Image(getClass().getResourceAsStream(editIcon))));
                                    break;
                                }
                                else if (translate.getStatus() == KeyStatus.REMOVED) {
                                    setGraphic(new ImageView(new Image(getClass().getResourceAsStream(deleteIcon))));
                                    break;
                                }
                            }
                        }
                    }

                };
            }
        });
        translationFolderLanguages.setCellFactory(new Callback<ListView<File>, ListCell<File>>() {
            @Override
            public ListCell<File> call(ListView<File> fileListView) {
                return new FilesListCell();
            }
        });
        translateKey.setCellValueFactory(new PropertyValueFactory<>("Key"));
        translateValue.setCellValueFactory(new PropertyValueFactory<>("Value"));
        translateEnglish.setCellValueFactory(new PropertyValueFactory<>("English"));
        translateValue.setCellFactory(tc -> new TextFieldTableCell<TranslateItem, String>(new DefaultStringConverter()));
        translateValue.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<TranslateItem, String>>() {
            @Override
            public void handle(TableColumn.CellEditEvent<TranslateItem, String> translateItemStringCellEditEvent) {
                TranslateItem item = translateItemStringCellEditEvent.getRowValue();
                item.setValue(translateItemStringCellEditEvent.getNewValue());
                item.markEdited();
                LanguageManager.unsavedChanges = true;

            }

        });

        translateEnglish.setCellValueFactory(cellData -> {
            //return LanguageManager.getCurrentEnglishString(cellData.getValue().getKey());

            return Bindings.createStringBinding(() -> {
               return LanguageManager.getCurrentEnglishString(cellData.getValue().getKey());
            });
        });

        translationTable.setRowFactory(tableView -> new TranslateItemRow() {

        });
        translationTable.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if(t.getButton() == MouseButton.SECONDARY) {
                    TranslateItem item = (TranslateItem) translationTable.getSelectionModel().getSelectedItem();
                    cm.getItems().clear();
                    if (item.getStatus() == KeyStatus.ADDED || item.getStatus() == KeyStatus.MODIFIED) {
                        cm.getItems().add(resolveString);
                    }
                    else if (item.getStatus() == KeyStatus.REMOVED) {
                        cm.getItems().add(removeString);
                        //cm.getItems().add(keepString);
                    }
                    //cm.getItems().add(openEditor);
                    cm.show(translationTable, t.getScreenX(), t.getScreenY());
                }
                else {
                    if (cm.isShowing()) {
                        cm.hide();
                    }
                }
            }
        });

        fileTreeView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (t.getButton() == MouseButton.PRIMARY) {
                    TreeItem<File> f = fileTreeView.getSelectionModel().getSelectedItems().get(0);
                    if (f != null && f.getValue() != null && !f.getValue().isDirectory() && fileMap != null && fileMap.containsKey(f)) {
                        translationTable.setItems(fileMap.get(f));
                        originalList = fileMap.get(f);
                        currentStage.setTitle("TranslationZed - " + f.getValue().getName());
                        currentFile = f;
                        filter(filterText.getText().toLowerCase());
                    }
                    else {
                        System.out.println("FAILED");
                        System.out.println(fileTreeView.getSelectionModel().getSelectedItems().get(0) == null);
                        System.out.println(fileMap.containsKey(f));
                    }
                }
            }
        });

        removeString.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TranslateItem item = (TranslateItem) translationTable.getSelectionModel().getSelectedItem();
                LanguageManager.removeKeyFromPrevious(item.getKey());
                translationTable.getItems().remove(translationTable.getSelectionModel().getSelectedItem());
                LanguageManager.unsavedChanges = true;
            }
        });

        resolveString.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TranslateItem item = (TranslateItem) translationTable.getSelectionModel().getSelectedItem();
                item.markEdited();
                item.setStatus(KeyStatus.NO_CHANGE);
                LanguageManager.setValueOfPreviousEnglishString(item.getKey(), LanguageManager.getCurrentEnglishString(item.getKey()));
                translationTable.refresh();
                LanguageManager.unsavedChanges = true;
            }
        });

        addString.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TreeItem<File> f = fileTreeView.getSelectionModel().getSelectedItems().get(0);
                TranslateItem item = (TranslateItem)translationTable.getSelectionModel().getSelectedItem();
                item.setStatus(KeyStatus.ADDED);

                if (f != null && item != null && f.getValue().getName().equals("Unused Strings") && fileMap.containsKey(f)) {
                    fileMap.get(f).add(item);
                    fileMap.get(currentFile).remove(item);
                    LanguageManager.addToMasterlist(item);
                }
            }
        });

        /*openEditor.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Parent root;
                try {
                    root = FXMLLoader.load(getClass().getResource("popUpEditor.fxml"));
                    Stage stage = new Stage();
                    stage.setTitle("String Editor");
                    stage.setScene(new Scene(root));
                    stage.show();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });*/

        ObservableList<String> filterOptions = FXCollections.observableArrayList();
        filterOptions.add("Key");
        filterOptions.add("Value");
        filterDropdown.setItems(filterOptions);

        filterDropdown.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String res = filterDropdown.getSelectionModel().getSelectedItem().toString().toLowerCase();

                if (res.equals("key"))
                    filterMode = FilterMode.KEY;
                else if (res.equals("value"))
                    filterMode = FilterMode.VALUE;
            }
        });

        filterText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                filter(filterText.getText().toLowerCase());
            }
        });

        generateTranslationReport.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                LanguageManager.generateTranslationReport(translationFolderLanguages);
            }
        });

        itemEnglishToggle.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (translateEnglish.isVisible())
                    translateEnglish.setVisible(false);
                else
                    translateEnglish.setVisible(true);
            }
        });
    }

    public File openLanguageFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");

        File selectedDirectory = chooser.showDialog((Stage)mainVBox.getScene().getWindow());
        return selectedDirectory;
    }

    public File openLanguageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select File");
        File selectedFile = chooser.showOpenDialog((Stage)mainVBox.getScene().getWindow());
        return selectedFile;
    }

    public void addFileItemConverter() {
        items.add(openLanguageFile());
    }

    public void addFolderItemConverter() {
        File file = openLanguageFolder();
        if (file != null) {
            items.add(file);
        }
    }

    public void convertItemFileToTranslation() {
        converter.convertFiles(items, false);
    }

    public void convertRecipeFileToTranslation() {
        converter.convertFiles(items, true);
    }

    public void convertItemsDotTxt(ActionEvent actionEvent) {
        for (int i = 0; i < rootItem.getChildren().size(); i++) {
            TreeItem<File> item = rootItem.getChildren().get(i);
            File file = item.getValue();
            if (file.getName().equalsIgnoreCase("Items_" + currentFolder.getName() + ".txt")) {
                ObservableList<TranslateItem> items = fileMap.get(item);
                converter.convertItemsDotTxt(this.items, currentFolder, currentCharset, items);
                break;
            }
        }
    }

    //TRANSLATE TAB CODE BELOW
    @Deprecated
    public void onTreeViewClick() {
        /*TreeItem<File> f = fileTreeView.getSelectionModel().getSelectedItems().get(0);
        if (f != null && f.getValue() != null && !f.getValue().isDirectory() && fileMap != null && fileMap.containsKey(f)) {
            translationTable.setItems(fileMap.get(f));
        }*/
    }

    public void onListViewClick() {
        if (translationFolderLanguages != null) {
            if (translationFolderLanguages.getSelectionModel().getSelectedItem() != null) {
                System.out.println(translationFolderLanguages.getSelectionModel().getSelectedItem().getName());
                loadLanguage(translationFolderLanguages.getSelectionModel().getSelectedItem());
                LanguageManager.unsavedChanges = false;
            }
        }
    }

    public void onEditColumn(TableColumn.CellEditEvent<TranslateItem, String> edit) {
        //edit.getRowValue().setValue(edit.getNewValue());
        //edit.getTableView().getItems().get(edit.getTablePosition().getRow()).setValue(edit.getNewValue());
    }

    public void saveTranslations() {
        boolean save = false;
        boolean error = false;
        boolean changeNotResolved = false;
        int deletion = 0;
        int modifications = 0;
        for (Map.Entry<TreeItem<File>, ObservableList<TranslateItem>> entry : fileMap.entrySet()) {
            List<String> lines = new ArrayList<>();
            lines.add(entry.getKey().getValue().getName().replace(".txt", "") + " = {");
            ArrayList<TranslateItem> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparing(TranslateItem::getKey));
            for (TranslateItem item : sorted) {
                if (!item.isIgnore() && item.getStatus() != KeyStatus.ADDED) {
                    lines.add("    " + item.getKey() + " = \"" + item.getValue() + "\",");
                }
                else if (item.getStatus() == KeyStatus.ADDED || item.getStatus() == KeyStatus.MODIFIED) {
                    if (item.isEdited()) {
                        changeNotResolved = true;
                    }
                }

                if (item.isIgnore() || item.isEdited()) {
                    if (item.isIgnore())
                        deletion++;
                    else if (item.isEdited())
                        modifications++;

                    save = true;
                }
            }
            lines.add("}");
            if (save) {
                Path file = Paths.get(entry.getKey().getValue().getAbsolutePath());
                try {
                    if (!Files.exists(file)) {
                        Files.createFile(file);
                    }
                    Files.write(file, lines, currentCharset);
                    System.out.println("Saving " + file.getFileName() + " with charset " + currentCharset);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    System.out.println(currentCharset.toString());
                    System.out.println(currentFolder.getName());
                    error = true;
                }

                fileTreeView.refresh();
                save = false;
            }
        }

        if (error) {
            LanguageManager.generateAlert("Save Error", null, "There was an error while saving translations.", Alert.AlertType.ERROR).showAndWait();
        }
        else {
            LanguageManager.generateAlert("Save Successful", null, "Modified " + modifications + " Language Strings\nDeleted " + deletion + " Language Strings", Alert.AlertType.INFORMATION).showAndWait();
            if (changeNotResolved) {
                System.out.println("CHANGE NOT RESOLVED!");
                LanguageManager.generateAlert("Changes Not Marked Resolved", null, "Added/Modified marked strings changed but not marked as resolved have NOT been saved.", Alert.AlertType.INFORMATION).showAndWait();
            }
        }
        LanguageManager.saveEnglishMap(currentFolder);
        LanguageManager.unsavedChanges = false;
    }

    public void saveFile(File f) {
        List<String> lines = new ArrayList<>();
        lines.add(f.getName().replace(".txt", "") + " {");
    }

    public void populateTranslationFolder() {
        File folder = openLanguageFolder();
        if (folder != null) {
            populateTranslationFolder(folder);
        }
    }

    public void populateTranslationFolder(File folder) {
        translationFolderLanguages.getItems().clear();
        translationFolderLanguages.setItems(LanguageManager.openTranslationFolder(folder));
    }

    public void loadLanguage(File folder) {
        fileMap.clear();
        rootItem = LanguageManager.parseTranslationFolder(fileMap, folder, true);
        fileTreeView.setRoot(rootItem);
        for (int i = 0; i < rootItem.getChildren().size(); i++) {
            TreeItem<File> item = rootItem.getChildren().get(i);
            if (item.getValue().isDirectory() && item.getChildren().isEmpty()) {
                rootItem.getChildren().remove(i);
                i--;
            }
        }
        System.out.println(rootItem.getValue().getAbsolutePath());
        Map<String, ObservableList<TranslateItem>> unusedMap = LanguageManager.checkUnaddedStrings();
        for (TreeItem<File> fileName : fileMap.keySet()) {
            String parsedFileName = fileName.getValue().getName().replace(rootItem.getValue().getName().trim() + ".txt", "");
            if (unusedMap.containsKey(parsedFileName)) {
                fileMap.get(fileName).addAll(unusedMap.get(parsedFileName));
                unusedMap.remove(parsedFileName);
            }
        }

        for (Map.Entry<String, ObservableList<TranslateItem>> item : unusedMap.entrySet()) {
            if (!LanguageManager.excludeFile(item.getKey())) {
                TreeItem<File> file = new TreeItem<>();
                file.setValue(new File(rootItem.getValue().getAbsolutePath() + File.separator + item.getKey().trim() + rootItem.getValue().getName().trim() + ".txt"));
                rootItem.getChildren().add(file);
                fileMap.put(file, item.getValue());
            }
        }

        currentFolder = folder;
        currentCharset = Charset.forName(LanguageManager.getCharset(currentFolder.getName()));
        LanguageManager.generateAlert("Load Successful", null, folder.listFiles().length + " files and folders loaded.", Alert.AlertType.INFORMATION).showAndWait();

    }

    public void setStage(Stage stage) {
        this.currentStage = stage;
    }

    public void filter(String text) {
        if (filterMode != FilterMode.NONE && !originalList.isEmpty()) {
            FilteredList<TranslateItem> filteredData = new FilteredList<>(originalList);
            filteredData.setPredicate(new Predicate<TranslateItem>() {
                @Override
                public boolean test(TranslateItem translateItem) {
                    if (filterMode == FilterMode.KEY) {
                        if (translateItem.getKey().toLowerCase().contains(text))
                            return true;
                    } else if (filterMode == FilterMode.VALUE) {
                        if (translateItem.getValue().toLowerCase().contains(text))
                            return true;
                    }

                    return false;
                }
            });
            translationTable.setItems(filteredData);
            System.out.println(text);
        }
    }
}
