package searchengine.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

@Service
public class LemmaService {

    private static final Logger logger = LoggerFactory.getLogger(LemmaService.class);

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    // Добавляем репозиторий для Index
    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    /**
     * Обрабатывает текст страницы: подсчитывает частоту встречаемости каждой леммы и 
     * сохраняет/обновляет записи в базе.
     * Дополнительно создаёт записи в таблице Index, связывающие лемму и страницу.
     * Обрабатываются только слова, состоящие исключительно из букв.
     *
     * При возникновении ошибки выбрасывается RuntimeException с подробным сообщением.
     *
     * @param page объект страницы, к которой привязаны леммы
     * @param text очищенный текст страницы
     * @param site сайт, к которому относится страница
     */
    public void processLemmas(Page page, String text, Site site) {
        try {
            logger.info("Начало обработки лемм для страницы с ID: {} и сайта: {}", page.getId(), site.getId());
            String[] words = text.split("\\W+");
            Map<String, Integer> lemmaCount = new HashMap<>();
            
            for (String word : words) {
                if (word.isEmpty()) continue;
                // Удаляем цифры из слова
                String cleanedWord = word.replaceAll("\\d", "");
                // Если после удаления цифр слово пустое или содержит символы, не являющиеся буквами, пропускаем его
                if (!cleanedWord.matches("^[\\p{L}]+$")) continue;
                // Отбрасываем слова короче 3 символов (это можно сделать также через параметр, если потребуется)
                if (cleanedWord.length() < 3) continue;
                String lemma = cleanedWord.toLowerCase();
                lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
            }
            
            logger.info("Найдено {} уникальных лемм", lemmaCount.size());
            
            for (Map.Entry<String, Integer> entry : lemmaCount.entrySet()) {
                String lemmaText = entry.getKey();
                int count = entry.getValue();
                logger.debug("Обработка леммы: {} (количество: {})", lemmaText, count);
        
                // 1. Сохраняем или обновляем лемму
                Lemma lemmaEntity = saveOrUpdateLemma(lemmaText, site);
        
                // 2. Создаём и сохраняем запись в таблице Index для связи леммы и страницы
                Index indexEntity = new Index();
                indexEntity.setLemmaId(lemmaEntity.getId());
                indexEntity.setPageId(page.getId());
                indexEntity.setLemma(lemmaText);
                // Используем количество вхождений как значение ранга
                indexEntity.setRank((float) count);
                // Пример: значение поля idx (может быть изменено по логике)
                indexEntity.setIdx(1);
        
                indexRepository.save(indexEntity);
                logger.debug("Сохранена запись в таблице Index: lemmaId={}, pageId={}, rank={}", 
                             lemmaEntity.getId(), page.getId(), indexEntity.getRank());
            }
            logger.info("Завершена обработка лемм для страницы с ID: {}", page.getId());
        } catch (Exception e) {
            logger.error("Ошибка при обработке лемм для страницы с ID {}: {}", page.getId(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при обработке лемм для страницы с ID " + page.getId() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Сохраняет лемму для указанного сайта по его идентификатору.
     * При возникновении ошибки выбрасывается RuntimeException с подробным сообщением.
     *
     * @param lemmaText текст леммы
     * @param siteId идентификатор сайта
     * @return сохранённая или обновлённая лемма
     */
    public Lemma saveLemma(String lemmaText, int siteId) {
        try {
            logger.info("Сохранение леммы: {} для сайта с ID: {}", lemmaText, siteId);
            
            // Проверка наличия сайта по идентификатору
            Site foundSite = siteRepository.findById(siteId)
                    .orElseThrow(() -> {
                        String errorMsg = "Сайт с ID " + siteId + " не существует";
                        logger.error(errorMsg);
                        return new IllegalArgumentException(errorMsg);
                    });
            
            // Поиск существующей леммы по тексту
            Lemma lemma = lemmaRepository.findByLemmaText(lemmaText);
            if (lemma == null) {
                logger.debug("Лемма {} не найдена, создаём новую", lemmaText);
                lemma = new Lemma(foundSite, lemmaText);
                lemma.setFrequency(1);
            } else {
                logger.debug("Лемма {} найдена, текущая частота: {}", lemmaText, lemma.getFrequency());
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            
            Lemma savedLemma = lemmaRepository.save(lemma);
            logger.info("Лемма {} сохранена/обновлена, новая частота: {}", lemmaText, savedLemma.getFrequency());
            return savedLemma;
        } catch (Exception e) {
            logger.error("Ошибка при сохранении леммы {} для сайта с ID {}: {}", lemmaText, siteId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при сохранении леммы " + lemmaText + " для сайта с ID " + siteId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Адаптерный метод для обратной совместимости.
     * Вызывает метод saveLemma с использованием идентификатора сайта.
     *
     * @param lemmaText текст леммы
     * @param site объект сайта, к которому относится лемма
     * @return сохранённая или обновлённая лемма
     */
    public Lemma saveOrUpdateLemma(String lemmaText, Site site) {
        return saveLemma(lemmaText, site.getId());
    }
    
    // Если нужна логика проверки сертификата, можно добавить здесь или в другом сервисе.
}

