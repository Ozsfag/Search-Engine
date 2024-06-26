package searchengine.utils.morphology;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;
import searchengine.config.MorphologySettings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * Util that responsible for morphology transformation
 * @author Ozsfag
 */
@Component
@RequiredArgsConstructor
public class Morphology {
    private final RussianLuceneMorphology russianLuceneMorphology;
    private final EnglishLuceneMorphology englishLuceneMorphology;
    private final MorphologySettings morphologySettings;
    /**
     * counts words in the transmitted text
     * @param content from page
     * @return the amount of words at page
     */
    public Map<String, Integer> wordCounter(String content) {
        Map<String, Integer> russianCounter = wordFrequency(content, morphologySettings.getNotCyrillicLetters(), russianLuceneMorphology, morphologySettings.getRussianParticleNames());
        Map<String, Integer> englishCounter = wordFrequency(content, morphologySettings.getNotLatinLetters(), englishLuceneMorphology, morphologySettings.getEnglishParticlesNames());
        return Stream.concat(russianCounter.entrySet().parallelStream(), englishCounter.entrySet().parallelStream())
                .parallel()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Integer> wordFrequency(String content, String regex, LuceneMorphology luceneMorphology, String[] particles){
        return Arrays.stream(content.toLowerCase().replaceAll(regex, morphologySettings.getEmptyString()).split(morphologySettings.getSplitter()))
                .parallel()
                .filter(word -> isNotParticle(word, luceneMorphology, particles))
//                .map(luceneMorphology::getNormalForms)
//                .flatMap(Collection::stream)
//                .map(forms -> forms.get(0))
//                .filter(Objects::nonNull)
                .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));
    }
    private boolean isNotParticle(String word, LuceneMorphology luceneMorphology, String[] particles) {
        return word.length() > 2 && !word.isBlank() && Arrays.stream(particles)
                .noneMatch(part -> luceneMorphology.getMorphInfo(word).contains(part));
    }
    /**
     * get unique words set from query
     * @param query, search query
     * @return unique set of lemma
     */
    public Set<String> getLemmaSet(String query) {
        String onlyLatinLetters = "[a-z]+";
        Stream<String> russianLemmaStream = getLemmaStreamByLanguage(query, morphologySettings.getNotCyrillicLetters(), russianLuceneMorphology,englishLuceneMorphology, morphologySettings.getRussianParticleNames(), onlyLatinLetters);
        String onlyCyrillicLetters = "[а-я]+";
        Stream<String> englishLemmaStream = getLemmaStreamByLanguage(query, morphologySettings.getNotLatinLetters(), englishLuceneMorphology, russianLuceneMorphology, morphologySettings.getEnglishParticlesNames(), onlyCyrillicLetters);
        return Stream.concat(russianLemmaStream.parallel(), englishLemmaStream.parallel()).collect(Collectors.toSet());

    }
    private Stream<String> getLemmaStreamByLanguage(String query, String nonLetters, LuceneMorphology luceneMorphology1, LuceneMorphology luceneMorphology2, String[] particles, String onlyLetters){
        return Arrays.stream(query.toLowerCase().replaceAll(nonLetters, morphologySettings.getEmptyString()).split(morphologySettings.getSplitter()))
                .filter(word -> isNotParticle(word, luceneMorphology1, particles))
                .flatMap(queryWord -> queryWord.matches(onlyLetters)?
                        luceneMorphology2.getNormalForms(queryWord).stream():
                        luceneMorphology1.getNormalForms(queryWord).stream());
    }

    /**
     * split transmitted link into scheme and host, and path
     *
     * @param url@return valid url components
     * @throws URISyntaxException
     */
    public String[] getValidUrlComponents(String url) throws URISyntaxException {
        final URI uri = new URI(url);
        final String schemeAndHost = uri.getScheme() + "://" + uri.getHost() + "/";
        final String path = uri.getPath();
        return new String[]{schemeAndHost, path};
    }
}



