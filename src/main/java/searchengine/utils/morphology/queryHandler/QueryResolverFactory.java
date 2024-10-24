package searchengine.utils.morphology.queryHandler;

import java.util.Collection;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import searchengine.config.MorphologySettings;
import searchengine.utils.validator.Validator;

@Component
public class QueryResolverFactory {
  @Autowired @Lazy private RussianLuceneMorphology russianLuceneMorphology;
  @Autowired @Lazy private EnglishLuceneMorphology englishLuceneMorphology;
  @Autowired @Lazy private MorphologySettings morphologySettings;
  @Autowired @Lazy private Validator validator;

  private volatile QueryResolver russianQueryResolver;
  private volatile QueryResolver englishQueryResolver;

  public QueryResolver createRussianQueryHandler() {
    if (russianQueryResolver == null) {
      synchronized (this) {
        if (russianQueryResolver == null) {
          russianQueryResolver =
              createQueryHandler(
                  morphologySettings.getNotCyrillicLetters(),
                  russianLuceneMorphology,
                  englishLuceneMorphology,
                  morphologySettings.getRussianParticleNames(),
                  morphologySettings.getOnlyLatinLetters());
        }
      }
    }
    return russianQueryResolver;
  }

  public QueryResolver createEnglishQueryHandler() {
    if (englishQueryResolver == null) {
      synchronized (this) {
        if (englishQueryResolver == null) {
          englishQueryResolver =
              createQueryHandler(
                  morphologySettings.getNotLatinLetters(),
                  englishLuceneMorphology,
                  russianLuceneMorphology,
                  morphologySettings.getEnglishParticlesNames(),
                  morphologySettings.getOnlyCyrillicLetters());
        }
      }
    }
    return englishQueryResolver;
  }

  private QueryResolver createQueryHandler(
      String nonLetters,
      LuceneMorphology primaryMorphology,
      LuceneMorphology secondaryMorphology,
      Collection particles,
      String onlyLetters) {
    return new QueryResolverImpl(
        nonLetters,
        primaryMorphology,
        secondaryMorphology,
        particles,
        onlyLetters,
        morphologySettings.getEmptyString(),
        morphologySettings.getSplitter(),
        validator);
  }
}
