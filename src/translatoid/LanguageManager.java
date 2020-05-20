package translatoid;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import sun.reflect.generics.tree.Tree;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LanguageManager {
    private static String currentLanguage = "NONE";

    private static Map<String, TranslateItem> currentEnglishMap;
    private static Map<String, TranslateItem> previousEnglishMap;
    private static Map<String, TranslateItem> masterList = new HashMap<>();
    private int modifiedStrings = 0;
    private int deletedStrings = 0;
    private int addedStrings = 0;
    private static Map<String, String> charsets = new HashMap<>();
    private static String fileName = "_englishcache_v1.properties";
    public static boolean unsavedChanges = false;
    //private static final Node addIcon = new ImageView(new Image(getClass().getResourceAsStream("media/icons/add.png")));

    public static ObservableList<File> openTranslationFolder(File folder) {
        boolean engFolderFound = false;
        ObservableList<File> results = FXCollections.observableArrayList();
        if (folder != null && folder.isDirectory()) {
            if (folder.listFiles() != null) {
                for (File f : folder.listFiles()) {
                    currentLanguage = folder.getName();
                    if (f.getName().equals("EN")) {
                        Map<TreeItem<File>, ObservableList<TranslateItem>> enMap = new HashMap<>();
                        engFolderFound = true;
                        parseTranslationFolder(enMap, f);
                        setupEnglishMap(enMap);
                    }
                    if (f.isDirectory() && (!f.getName().equals(".git") && !f.getName().equals("EN"))) {
                        results.add(f);
                    }
                }
            }
        }

        if (!engFolderFound) {
            results.clear();
            Alert alert = LanguageManager.generateAlert("No Language Folders Found", null, "No English folder detected. Check you are opening the correct folder and try again.", Alert.AlertType.ERROR);
            alert.showAndWait();
        }

        return results;
    }

    public static void setupEnglishMap(Map<TreeItem<File>, ObservableList<TranslateItem>> map) {
        currentEnglishMap = new HashMap<>();
        for (Map.Entry<TreeItem<File>, ObservableList<TranslateItem>> it : map.entrySet()) {
            for (TranslateItem item : it.getValue()) {
                currentEnglishMap.put(item.getKey(), item);
            }
        }
    }

    public static void loadEnglishMap(File folder) {
        try {
            File file = new File(folder.getAbsolutePath() + File.separator + currentLanguage + fileName);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(folder.getAbsolutePath() + File.separator + currentLanguage + fileName);
                ObjectInputStream ois = new ObjectInputStream(fis);
                previousEnglishMap = new HashMap<>();
                previousEnglishMap = (HashMap) ois.readObject();
                System.out.println("English cache file exists.");
            }
            else {
                System.out.println("Cache file does not exist!");
                saveEnglishMap(folder);
                previousEnglishMap = currentEnglishMap;
            }
        } catch (Exception ex) {
            System.err.println("Error loading English Map");
            System.err.println(ex);
        }
    }

    public static void saveEnglishMap(File folder) {
        try {
            FileOutputStream fos = new FileOutputStream(folder.getAbsolutePath() + File.separator + currentLanguage + fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            if (previousEnglishMap != null) {
                oos.writeObject(previousEnglishMap);
            }
            else {
                oos.writeObject(currentEnglishMap);
            }
            oos.close();
        } catch (Exception ex) {
            System.err.println("Error saving English Map");
            System.err.println(ex.getMessage());
        }
    }

    public static TreeItem<File> parseTranslationFolder(Map<TreeItem<File>, ObservableList<TranslateItem>> map, File folder) {
        return parseTranslationFolder(map, folder, false);
    }

    public static TreeItem<File> parseTranslationFolder(Map<TreeItem<File>, ObservableList<TranslateItem>> map, File folder, boolean loadEnglish) {
        map.clear();
        TreeItem<File> item = null;
        if (folder != null && folder.isDirectory()) {
            if (folder.listFiles() != null) {
                currentLanguage = folder.getName();
                if (loadEnglish)
                    loadEnglishMap(folder);
                item = new TreeItem<>(folder);
                item.setExpanded(true);
                masterList.clear();
                populateTreeItems(folder, item, map);
            }
        }

        return item;
    }

    public static void populateTreeItems(File folder, TreeItem<File> root, Map<TreeItem<File>, ObservableList<TranslateItem>> map) {
        if (folder != null && folder.isDirectory()) {
            if (folder.listFiles() != null) {
                for (File f : Objects.requireNonNull(folder.listFiles())) {
                    TreeItem<File> item = new TreeItem<>(f);
                    if (f.isDirectory()) {
                        populateTreeItems(f, item, map);
                        root.getChildren().add(item);
                    } else {
                        ObservableList<TranslateItem> list = parseTranslationFile(item);
                        if (!list.isEmpty()) {
                            map.put(item, list);
                            root.getChildren().add(item);
                        }
                        for (TranslateItem itm : list) {
                            masterList.put(itm.getKey(), itm);
                        }
                    }
                }
            }
        }

        int numOfStrings = 0;
        int untranslatedStrings = 0;
        if (currentEnglishMap != null) {
            for (Map.Entry<String, TranslateItem> entry : currentEnglishMap.entrySet()) {
                numOfStrings++;
                if (masterList.containsKey(entry.getKey())) {
                    TranslateItem item = masterList.get(entry.getKey());
                    if (item.getValue().equals(entry.getValue().getValue()) || entry.getValue().getValue().isEmpty()) {
                        untranslatedStrings++;
                    }
                } else {
                    untranslatedStrings++;
                }
            }
        }
    }

    public static ObservableList<TranslateItem> parseTranslationFile(TreeItem<File> item) {
        ObservableList<TranslateItem> list = FXCollections.observableArrayList();
        File f = item.getValue();
        if (f != null && !f.isDirectory() && !excludeFile(f.getName())) {
            try {
                BufferedReader br = Files.newBufferedReader(Paths.get(f.getAbsolutePath()), Charset.forName(charsets.get(currentLanguage)));
                String line;
                String[] lineArr;
                while ((line = br.readLine()) != null) {
                    if (!line.contains("{") && !line.contains("}")) {
                        lineArr = line.split("=");
                        if (lineArr.length >= 2) {
                            lineArr[1] = lineArr[1].trim();
                            lineArr[1] = lineArr[1].replace("\"", "");
                            if (lineArr[1].length() > 0 && lineArr[1].charAt(lineArr[1].length()-1) == ',') {
                                lineArr[1] = lineArr[1].substring(0, lineArr[1].length()-1);
                            }
                            lineArr[0] = lineArr[0].trim();
                            String fileName = item.getValue().getName().replace(currentLanguage + ".txt", "");
                            TranslateItem translate = new TranslateItem(lineArr[0], lineArr[1], fileName);
                            list.add(translate);
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }

        return list;
    }

    public static Map<String, ObservableList<TranslateItem>> checkUnaddedStrings() {
        Map<String, ObservableList<TranslateItem>> map = new HashMap<>();
        for (Map.Entry<String, TranslateItem> item : currentEnglishMap.entrySet()) {
            if (!masterList.containsKey(item.getValue().getKey())) {
                TranslateItem newItem = new TranslateItem(item.getValue().getKey(), item.getValue().getValue(), item.getValue().getFileName());
                newItem.setStatus(KeyStatus.ADDED);
                if (map.containsKey(item.getValue().getFileName())) {
                    map.get(item.getValue().getFileName()).add(newItem);
                }
                else {
                    map.put(item.getValue().getFileName(), FXCollections.observableArrayList());
                    map.get(item.getValue().getFileName()).add(newItem);
                }
            }
        }
        return map;
    }

    public static KeyStatus checkKey(String key) {
        if (currentEnglishMap != null && previousEnglishMap != null) {
            if (!currentEnglishMap.containsKey(key) && previousEnglishMap.containsKey(key)) {
                return KeyStatus.REMOVED;
            }

            if ((currentEnglishMap.containsKey(key) && previousEnglishMap.containsKey(key)) && !previousEnglishMap.get(key).getValue().equals(currentEnglishMap.get(key).getValue())) {
                return KeyStatus.MODIFIED;

            }

            if (currentEnglishMap.containsKey(key) && !previousEnglishMap.containsKey(key)) {
                return KeyStatus.ADDED;
            }
        }

        return KeyStatus.NO_CHANGE;
    }

    public static String getCurrentEnglishString(String key) {
        if (currentEnglishMap.containsKey(key)) {
            return currentEnglishMap.get(key).getValue();
        }

        return null;
    }

    public static String getPreviousEnglishString(String key) {
        if (previousEnglishMap != null) {
            if (previousEnglishMap.containsKey(key)) {
                return previousEnglishMap.get(key).getValue();
            }
        }

        return null;
    }

    public static boolean removeKeyFromPrevious(String key) {
        if (previousEnglishMap.containsKey(key)) {
            previousEnglishMap.remove(key);
            return true;
        }

        return false;
    }

    public static void setValueOfPreviousEnglishString(String key, String value) {
        if (previousEnglishMap.containsKey(key)) {
            previousEnglishMap.get(key).setValue(value);
        }
        else {
            previousEnglishMap.put(key, new TranslateItem(key, value));
        }
    }

    public static void addToMasterlist(TranslateItem item) {
        masterList.put(item.getKey(), item);
    }

    public static boolean excludeFile(String file) {
        if (file.equals("News_") || file.equals(currentLanguage + fileName)) {
            return true;
        }

        return false;
    }

    public static Alert generateAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(content);
        alert.setTitle(title);
        alert.setHeaderText(header);

        return alert;
    }

    public static void initCharsets() {
        charsets.put("EN", "UTF-8");
        charsets.put("AF", "Cp1252");
        charsets.put("AR", "Cp1252");
        charsets.put("CH", "Big5");
        charsets.put("CN", "UTF-8");
        charsets.put("CS", "Cp1250");
        charsets.put("DA", "Cp1252");
        charsets.put("DE", "Cp1252");
        charsets.put("EE", "UTF-8");
        charsets.put("ES", "Cp1252");
        charsets.put("FR", "Cp1252");
        charsets.put("HU", "Cp1252");
        charsets.put("IT", "Cp1252");
        charsets.put("JP", "UTF-8");
        charsets.put("KO", "UTF-16");
        charsets.put("NL","Cp1252");
        charsets.put("NO", "Cp1252");
        charsets.put("PL", "Cp1250");
        charsets.put("PT", "Cp1252");
        charsets.put("PTBR", "Cp1252");
        charsets.put("RU", "Cp1251");
        charsets.put("TH", "Cp1252");
        charsets.put("TR", "Cp1254");

        File file = new File("charset_override.txt");
        if (file.exists()) {
            try {
                BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()), Charset.forName("UTF-8"));
                String line;
                String[] parsed;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!(line.charAt(0) == '/' && line.charAt(1) == '/')) {
                        parsed = line.split("=");
                        if (parsed.length > 1) {
                            System.out.println("OVERRIDE " + parsed[0] + " WITH " + parsed[1]);
                            charsets.put(parsed[0], parsed[1]);
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
        else {
            try {
                new FileOutputStream("charset_override.txt", true).close();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    public static String getCharset(String lang) {
        if (charsets.containsKey(lang)) {
            return charsets.get(lang);
        }

        return "UTF-8";
    }

    public static void generateTranslationReport(ListView<File> languages) {
        JsonObject obj = new JsonObject();

        try {
            ArrayList<TranslateItem> untranslatedList = new ArrayList<>();
            int stringCount = 0;
            int translatedString = 0;
            obj.setTotalStrings(currentEnglishMap.size());
            Map<TreeItem<File>, ObservableList<TranslateItem>> map = new HashMap<>();
            for (File f : languages.getItems()) {
                if (f != null && !f.getName().equals("EN") && f.isDirectory()) {
                    System.out.println(f.getName());
                    LanguageObject lang = new LanguageObject();
                    lang.setLanguageCode(f.getName());
                    lang.setLanguage(parseLanguageCode(f.getName()));
                    parseTranslationFolder(map, f);
                    translatedString = 0;
                    for (Map.Entry<String, TranslateItem> s : masterList.entrySet()) {
                        if (currentEnglishMap.get(s.getKey()).getValue().equals(s.getValue().getValue()) || s.getValue().getValue().isEmpty()) {
                            lang.addString(s.getValue());
                        }
                        else {
                            translatedString++;
                        }
                    }
                    obj.addLanguage(lang);
                }
            }

            obj.write();

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static String parseLanguageCode(String code) {
        String result = "";
        switch (code.toLowerCase()) {
            case "en":
                result = "English";
                break;
            case "af":
                result = "Afrikaans";
                break;
            case "ar":
                result = "Spanish (Argentina)";
                break;
            case "cn":
                result = "Chinese (Simplified)";
                break;
            case "cs":
                result = "Czech";
                break;
            case "da":
                result = "Danish";
                break;
            case "de":
                result = "German";
                break;
            case "es":
                result = "Spanish";
                break;
            case "fr":
                result = "French";
                break;
            case "hu":
                result = "Hungarian";
            case "it":
                result = "Italian";
                break;
            case "jp":
                result = "Japanese";
                break;
            case "ko":
                result = "Korean";
                break;
            case "nl":
                result = "Dutch";
                break;
            case "no":
                result = "Norweigan";
                break;
            case "pl":
                result = "Polish";
                break;
            case "pt":
                result = "Portuguese";
                break;
            case "ptbr":
                result = "Brazilian Portuguese";
                break;
            case "ru":
                result = "Russian";
                break;
            case "th":
                result = "Thai";
                break;
            case "tr":
                result = "Turkish";
                break;

        }

        return result;
    }

}
