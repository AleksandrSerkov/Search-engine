import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLetterDecoderEncoder;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class Main {
    public static void main(String[] args) throws IOException {
        LemmaFinder counter = new LemmaFinder();
        String[] words = {"или", "и", "копал", "копать", "хитро", "хитрый", "синий"};


        for (String word : words) {
            HashMap<String, String> result = counter.getLemmaInfo(word);

            for (String lemma : result.keySet()) {
                String info = result.get(lemma);
                System.out.println(lemma + "|" + info);
            }

            if (result.isEmpty()) {
                System.out.println("Для слова " + word + " не найдено информации");
            }
        }
    }
}