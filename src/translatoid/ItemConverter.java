package translatoid;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ItemConverter {

    private ArrayList<String> result;
    private HashMap<String, String> map;
    private String charset = "windows-1252";
    int count;
    Path path;

    public void convertFiles(ObservableList<File> files, boolean findRecipe) {
        count = 0;
        result = new ArrayList<>();
        map = new HashMap<>();
        if (!findRecipe) {
            path = Paths.get("Items_empty.txt");
            result.add("Items_EN = {");
        }
        else {
            path = Paths.get("Recipe_empty.txt");
            result.add("Recipe_EN = {");
        }

        for (File f : files) {
            if (findRecipe)
                parseFile(f, true);
            else
                parseFile(f, false);
        }

        for (Map.Entry element : map.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(element.getKey());
            sb.append(" = ");
            sb.append("\"");
            sb.append(element.getValue());
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

    public void parseFile(File f, boolean findRecipe) {
        try {
            if (!f.isDirectory()) {
                BufferedReader reader = Files.newBufferedReader(Paths.get(f.getAbsolutePath()));
                String line;
                String[] lineArr;
                boolean inItem = false;
                boolean foundDisplayName = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!inItem) {
                        lineArr = line.split(" ");
                        if (lineArr[0].equals("item") && !findRecipe) {
                            inItem = true;
                        }
                        else if (lineArr[0].equals("recipe") && findRecipe) {
                            StringBuilder sb = new StringBuilder();
                            StringBuilder recipeName = new StringBuilder();
                            sb.append("Recipe");
                            for (String s : lineArr) {
                                if (!s.equals("recipe")) {
                                    sb.append("_");
                                    sb.append(s);
                                    if (recipeName.length() == 0) {
                                        recipeName.append(s);
                                    }
                                    else {
                                        recipeName.append(" ");
                                        recipeName.append(s);
                                    }
                                }
                            }

                            map.put(sb.toString(), recipeName.toString());
                            count++;
                        }
                    } else {
                        if (line.equals("}")) {
                            inItem = false;
                            foundDisplayName = false;
                        } else {
                            if (!foundDisplayName) {
                                lineArr = line.split("=");
                                if (lineArr.length >= 2) {
                                    lineArr[0] = lineArr[0].trim();
                                    if (lineArr[0].equals("DisplayName")) {
                                        lineArr[1] = lineArr[1].trim();
                                        String itemName = lineArr[1].replace(",", "");
                                        String formatted = "DisplayName_" + lineArr[1].replace(' ', '_').replace(",", "").replace("-", "_");
                                        map.put(formatted, itemName);
                                        foundDisplayName = true;
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                if (f.listFiles() != null) {
                    for (File file : f.listFiles()) {
                        parseFile(file, findRecipe);
                    }
                }
            }

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception Dialog");
            alert.setHeaderText("An exception was hit while parsing the file!");
            alert.setContentText(e.toString());
            alert.showAndWait();
        }
    }

    public void parseFile(File f) {
        parseFile(f, false);
    }
}
