package translatoid;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ItemConverter {

    private ArrayList<String> result;
    private HashMap<String, String> map;
    private final String charset = "windows-1252";
    int count;
    Path path;
    private final StringBuilder m_stringBuilder = new StringBuilder();
    private boolean m_findRecipe;
    private String m_moduleName;

    public void convertFiles(ObservableList<File> files, boolean findRecipe) {
        count = 0;
        result = new ArrayList<>();
        map = new HashMap<>();
        if (!findRecipe) {
            path = Paths.get("ItemName_empty.txt");
            result.add("ItemName_EN = {");
        }
        else {
            path = Paths.get("Recipe_empty.txt");
            result.add("Recipe_EN = {");
        }

        for (File f : files) {
            parseFileOrFolder(f, findRecipe);
        }
        
        ArrayList<String> sorted = new ArrayList<>(map.keySet());
        Collections.sort(sorted);

        for (String key : sorted) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(key);
            sb.append(" = ");
            sb.append("\"");
            sb.append(map.get(key));
            sb.append("\",");
            result.add(sb.toString());
        }

        result.add("}");
        Alert alert;
        try {
            Files.write(path, result, Charset.forName(charset));
        } catch (Exception e) {
            alert = LanguageManager.generateAlert("Exception Dialog", "An exception was hit while writing the file!", e.toString(), Alert.AlertType.ERROR);
            alert.showAndWait();
        }

        alert = LanguageManager.generateAlert("Operation Complete", null, "Parsed " + count + " lines of data.", Alert.AlertType.INFORMATION);
        alert.showAndWait();
    }

    /**
     * Convert Items_XX.txt to ItemName_XX.txt for the currently-selected language.
     * @param files
     * @param languageFolder
     */
    public void convertItemsDotTxt(ObservableList<File> files, File languageFolder, Charset charSet, ObservableList<TranslateItem> items) {
        count = 0;
        map = new HashMap<>();

        // Fill map with "ItemName_Module.type = DisplayName".
        for (File f : files) {
            parseFileOrFolder(f, false);
        }

        ArrayList<String> sorted = new ArrayList<>(map.keySet());
        Collections.sort(sorted);
        
        HashMap<String, String> DisplayNames = new HashMap<>();
        for (TranslateItem item : items) {
            // key: DisplayName_<mangled english name>
            // value: translated item name
            DisplayNames.put(item.getKey(), item.getValue());
        }

        result = new ArrayList<>();
        result.add("ItemName_" + languageFolder.getName() + " = {");

        StringBuilder sb = new StringBuilder();

        for (String key : sorted) {
            String key2 = map.get(key).replace(' ', '_').replace(",", "").replace("-", "_");
            String DisplayName = DisplayNames.get("DisplayName_" +  key2);
            if (DisplayName == null) {
                DisplayName = DisplayNames.get("DisplayName" +  key2);
                if (DisplayName == null) {
                    continue;
                }
            }
            sb.setLength(0);
            sb.append("    ");
            sb.append(key);
            sb.append(" = ");
            sb.append("\"");
            sb.append(DisplayName);
            sb.append("\",");
            result.add(sb.toString());
        }

        result.add("}");

        Alert alert;
        try {
            path = Paths.get(languageFolder.getAbsolutePath()).resolve("ItemName_" + languageFolder.getName() + ".txt");
            Files.write(path, result, charSet);
        } catch (Exception e) {
            alert = LanguageManager.generateAlert("Exception Dialog", "An exception was hit while writing the file!", e.toString(), Alert.AlertType.ERROR);
            alert.showAndWait();
        }

        alert = LanguageManager.generateAlert("Operation Complete", null, "Parsed " + count + " lines of data.", Alert.AlertType.INFORMATION);
        alert.showAndWait();
    }
    
    private void parseItem(String token) {
        String[] tokens = token.split("[{}]");
        String name = tokens[0];
        name = name.replace("item", "");
        name = name.trim();
        String[] keyValues = tokens[1].split(",");
        for (String keyValue : keyValues) {
            if (keyValue.trim().length() == 0) {
                continue;
            }
            if (!keyValue.contains("=")) {
                continue;
            }
            String[] ss = keyValue.split("=");
            String key = ss[0].trim();
            String value = ss[1].trim();
            if ("DisplayName".equalsIgnoreCase(key)) {
                String formatted = "DisplayName_" + value.replace(' ', '_').replace(",", "").replace("-", "_");
                formatted = "ItemName_" + m_moduleName + "." + name;
                map.put(formatted, value);
                count++;
            }
        }
    }

    private void parseRecipe(String token) {
        String tokens[] = token.split("[{}]");
        String name = tokens[0];
        name = name.replace("recipe", "");
        name = name.trim();
        map.put("Recipe_" + name.replaceAll(" ", "_"), name);
        count++;
    }

    private void parseModuleToken(String token) {
        token = token.trim();
        if (m_findRecipe) {
            if (token.indexOf("recipe") == 0) {
                parseRecipe(token);
            }
        } else {
            if (token.indexOf("item") == 0) {
                parseItem(token);
            }
        }
    }

    private void parseModule(String moduleName, String token) {
        m_moduleName = moduleName;
        ArrayList<String> tokens = ScriptParser.parseTokens(token);
        for (int n = 0; n < tokens.size(); n++) {
            String token1 = tokens.get(n);
            parseModuleToken(token1);
        }
    }

    private void parseFile(String token) {
        token = token.trim();
        if (token.indexOf("module") == 0) {
            int firstopen = token.indexOf("{");
            int lastClose = token.lastIndexOf("}");
            String[] ss = token.split("[{}]");
            String name = ss[0];
            name = name.replace("module", "");
            name = name.trim();
            String token1 = token.substring(firstopen + 1, lastClose);
            parseModule(name, token1);
        }
    }

    private void parseFile(File f) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(f.getAbsolutePath()))) {
            m_stringBuilder.setLength(0);
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                m_stringBuilder.append(inputLine);
                m_stringBuilder.append('\n');
            }
            String totalFile = m_stringBuilder.toString();
            totalFile = ScriptParser.stripComments(totalFile);
            ArrayList<String> Tokens = ScriptParser.parseTokens(totalFile);
            for (int n = 0; n < Tokens.size(); n++) {
                String token = Tokens.get(n);
                parseFile(token);
            }
        }
    }

    public void parseFileOrFolder(File f, boolean findRecipe) {
        m_findRecipe = findRecipe;
        try {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File file : files) {
                        parseFileOrFolder(file, findRecipe);
                    }
                }
            } else {
                parseFile(f);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception Dialog");
            alert.setHeaderText("An exception was hit while parsing the file!");
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
    }

    public void parseFileOrFolder(File f) {
        parseFileOrFolder(f, false);
    }
}
