package translatoid;

import java.util.ArrayList;

public class LanguageObject {
    private String language = "UNDEFINED";
    private String languageCode = "UN";
    private int untranslatedStrings = 0;
    private ArrayList<TranslateItem> stringList;

    LanguageObject() {
        stringList = new ArrayList<>();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String lang) {
        language = lang;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String code) {
        languageCode = code;
    }

    public int getUntranslatedStrings() {
        return untranslatedStrings;
    }

    public void setUntranslatedStrings(int strings) {
        untranslatedStrings = strings;
    }

    public void addString(TranslateItem string) {
        stringList.add(string);
    }

    public ArrayList<TranslateItem> getStrings() {
        return stringList;
    }
}
