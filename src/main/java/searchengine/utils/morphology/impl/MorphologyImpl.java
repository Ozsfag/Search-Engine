package searchengine.utils.morphology.impl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.factory.QueryResolverFactory;
import searchengine.factory.WordsCounterFactory;
import searchengine.utils.morphology.Morphology;
import searchengine.utils.morphology.queryHandler.QueryResolver;
import searchengine.utils.morphology.wordCounter.WordCounter;

/**
 * Util that responsible for morphology transformation
 *
 * @author Ozsfag
 */
@Component
public class MorphologyImpl implements Morphology {
  @Autowired private WordsCounterFactory wordsCounterFactory;
  @Autowired private QueryResolverFactory queryResolverFactory;

  @Override
  public Map<String, Integer> countWordFrequencyByLanguage(String content) {
    Map<String, Integer> result = new HashMap<>();

    WordCounter russianCounter = wordsCounterFactory.createRussianWordCounter();
    WordCounter englishCounter = wordsCounterFactory.createEnglishWordCounter();
    Map<String, Integer> englishWordFrequency = englishCounter.countWordsFromContent(content);
    Map<String, Integer> russianWordFrequency = russianCounter.countWordsFromContent(content);

    result.putAll(englishWordFrequency);
    result.putAll(russianWordFrequency);
    return result;
  }

  public Collection<String> getUniqueLemmasFromSearchQuery(String query) {
    QueryResolver russianQueryResolver = queryResolverFactory.createRussianQueryHandler();
    QueryResolver englishQueryResolver = queryResolverFactory.createEnglishQueryHandler();
    Stream<String> russianLemmaStream = russianQueryResolver.getLemmasFromQuery(query);
    Stream<String> englishLemmaStream = englishQueryResolver.getLemmasFromQuery(query);

    return Stream.concat(russianLemmaStream.parallel(), englishLemmaStream.parallel())
        .collect(Collectors.toSet());
  }
}
