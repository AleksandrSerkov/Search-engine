package searchengine.services;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
public class LemmaFinder {
    public HashMap<String, String> getLemmaInfo(String word) throws IOException {
        RussianLuceneMorphology luceneMorph = new RussianLuceneMorphology();
        HashMap<String, String> lemmaInfo = new HashMap<>();

        List<String> wordBaseForms = luceneMorph.getMorphInfo(word.toLowerCase());
        if (!wordBaseForms.isEmpty()) {
            String lemma = wordBaseForms.get(0).split("\\|")[0]; // Получаем лемму
            String info = wordBaseForms.get(0).split("\\|")[1]; // Получаем информацию о части речи

            lemmaInfo.put(lemma, info);
        }

        return lemmaInfo;
    }
}