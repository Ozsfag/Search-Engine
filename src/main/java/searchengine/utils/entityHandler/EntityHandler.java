package searchengine.utils.entityHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.dto.indexing.Site;
import searchengine.dto.indexing.responseImpl.ConnectionResponse;
import searchengine.exceptions.OutOfSitesConfigurationException;
import searchengine.exceptions.StoppedExecutionException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.connectivity.Connection;
import searchengine.utils.morphology.Morphology;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static searchengine.services.indexing.IndexingImpl.isIndexing;

/**
 * Util that handle and process kind of entities
 * @author Ozsfag
 */
@Component
@RequiredArgsConstructor
public class EntityHandler {
    private final Connection connection;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    public final Morphology morphology;
    private final PageRepository pageRepository;

    /**
     * @param href from application.yaml
     * @return indexed siteModel
     */
    public SiteModel getIndexedSiteModel(String href) {
        try {
            String validatedUrl = morphology.getValidUrlComponents(href)[0];

            Site site = sitesList.getSites().stream()
                    .filter(s -> validatedUrl.startsWith(s.getUrl()))
                    .findFirst()
                    .orElseThrow(() -> new OutOfSitesConfigurationException("Out of sites"));

            SiteModel siteModel = Optional.ofNullable(siteRepository.findByUrl(validatedUrl))
                    .orElseGet(()-> createSiteModel(site));

            return siteRepository.saveAndFlush(siteModel);

        } catch (URISyntaxException | OutOfSitesConfigurationException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    /**
     * get indexed PageModel from Site content
     * @param siteModel, from database
     * @param href of page from site
     * @return indexed pageModel
     */
    public PageModel getPageModel(SiteModel siteModel, String href) {
        PageModel pageModel = null;
        try {
            pageModel = createPageModel(siteModel, href);
            if (!isIndexing)throw new StoppedExecutionException("Индексация остановлена пользователем");
            return pageModel;

        } catch (StoppedExecutionException e) {
            pageRepository.saveAndFlush(Objects.requireNonNull(pageModel));
            throw new StoppedExecutionException(e.getLocalizedMessage());
        }
    }


    /**
     * Retrieves the indexed LemmaModel list from the content of a SiteModel.
     *
     * @param  siteModel    the SiteModel containing the content
     * @param  wordCountMap a map of word frequencies in the content
     * @return              the set of indexed LemmaModels
     */
    public Set<LemmaModel> getIndexedLemmaModelListFromContent( SiteModel siteModel, Map<String, Integer> wordCountMap) {

        Map<String, LemmaModel> existingLemmaModels = lemmaRepository.findByLemmaInAndSite_Id(new ArrayList<>(wordCountMap.keySet()), siteModel.getId())
                .parallelStream()
                .collect(Collectors.toMap(LemmaModel::getLemma, lemmaModel -> lemmaModel));


        return wordCountMap.entrySet().parallelStream().map(entry -> {
            String word = entry.getKey();
            int frequency = entry.getValue();

            return Optional.ofNullable(existingLemmaModels.get(word))
                    .map(lemmaModel -> {
                        lemmaModel.setFrequency(lemmaModel.getFrequency() + frequency);
                        return lemmaModel;
                    })
                    .orElseGet(() ->createLemmaModel(siteModel, word, frequency));

        }).collect(Collectors.toSet());
    }

    /**
     * Retrieves the IndexModel list from the content of a PageModel.
     *
     * @param  pageModel    the PageModel to retrieve indexes from
     * @param  lemmas       the set of LemmaModels to search for in the content
     * @param  wordCountMap a map of word frequencies in the content
     * @return the list of IndexModels generated from the content
     */
    public List<IndexModel> getIndexModelFromContent(PageModel pageModel, Set<LemmaModel> lemmas, Map<String, Integer> wordCountMap) {
        return wordCountMap.entrySet().stream().parallel()
                .map(word2Count -> {
                    try {
                        LemmaModel lemmaModel = lemmas.stream().filter(lemma -> lemma.getLemma().equals(word2Count.getKey())).findFirst().get();
                        return getIndexModel(lemmaModel, pageModel, (float) word2Count.getValue());
                    } catch (StoppedExecutionException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                    }
                })
                .toList();
    }
    private IndexModel getIndexModel(LemmaModel lemmaModel, PageModel pageModel, Float frequency) throws StoppedExecutionException {
        if (!isIndexing)throw new StoppedExecutionException("Stop indexing signal received");
        return Optional.ofNullable(indexRepository.findByLemmaAndPage(pageModel.getId(), lemmaModel.getId()))
                .map(indexModel -> {
                    indexModel.setRank(indexModel.getRank() + frequency);
                    return indexModel;})
                .orElseGet(() -> createIndexModel(pageModel, lemmaModel, frequency));
    }
    private SiteModel createSiteModel(Site site) {
        return SiteModel.builder()
                .status(Status.INDEXING)
                .url(site.getUrl())
                .statusTime(new Date())
                .lastError("")
                .name(site.getName())
                .build();
    }
    private PageModel createPageModel(SiteModel siteModel, String path){
        ConnectionResponse connectionResponse = connection.getConnectionResponse(path);
        return PageModel.builder()
                .site(siteModel).path(path)
                .code(connectionResponse.getResponseCode())
                .content(connectionResponse.getContent())
                .build();
    }
    private LemmaModel createLemmaModel(SiteModel siteModel, String lemma, int frequency){
        return LemmaModel.builder()
                .site(siteModel)
                .lemma(lemma)
                .frequency(frequency)
                .build();
    }
    private IndexModel createIndexModel(PageModel pageModel, LemmaModel lemmaModel, Float ranking){
        return IndexModel.builder()
                .page(pageModel)
                .lemma(lemmaModel)
                .rank(ranking)
                .build();
    }
}