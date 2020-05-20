package translatoid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;

public class JsonObject {
    int totalStrings = 0;
    ArrayList<LanguageObject> languages;
    JsonObject() {
        languages = new ArrayList<>();
    }

    public void write() {
        try {
            File file = new File("translation_report.json");
            file.createNewFile();
            Writer writer = new FileWriter(file);
            writer.write("{\n");
            writer.write("  \"totalStrings\": " + totalStrings + "\n");
            writer.write("  \"languages\": [\n");

            for (int i = 0; i < languages.size(); i++) {
                LanguageObject obj = languages.get(i);
                writer.write("      \"language\": \"" + obj.getLanguage() + "\",\n");
                writer.write("      \"code\": \"" + obj.getLanguageCode() + "\",\n");
                writer.write("      \"untranslatedStrings\": " + obj.getUntranslatedStrings() + "\n");
                writer.write("      \"stringList\": [\n" );
                for (int x = 0; x < languages.get(i).getStrings().size(); x++) {
                    TranslateItem item = languages.get(i).getStrings().get(x);
                    writer.write("          {\n");
                    writer.write("              \"string_key\": \"" + item.getKey() + "\",\n");
                    writer.write("              \"string_value\": \"" + item.getValue() + "\" \n");
                    writer.write("          }");
                    if (x != languages.get(i).getStrings().size()-1) {
                        writer.write(",\n");
                    }
                    else {
                        writer.write("\n");
                    }
                }
                writer.write("          ]\n");
                writer.write("      }");
                if (i != languages.size()-1) {
                    writer.write(",\n");
                }
                else {
                    writer.write("\n");
                }
            }
            writer.write("  ]\n");
            writer.write("}");

            writer.flush();
            writer.close();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public int getTotalStrings() {
        return totalStrings;
    }

    public void setTotalStrings(int strings) {
        totalStrings = strings;
    }

    public void addLanguage(LanguageObject obj) {
        languages.add(obj);
    }
}
