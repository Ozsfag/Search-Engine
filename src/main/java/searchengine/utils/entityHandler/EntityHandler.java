package searchengine.utils.entityHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
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

            SiteModel siteModel = Optional.ofNullable(siteRepository.findSiteByUrl(validatedUrl))
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
    public Collection<LemmaModel> getIndexedLemmaModelListFromContent( SiteModel siteModel, Map<String, Integer> wordCountMap) {

        Set<LemmaModel> existingLemmaModels =
                lemmaRepository.findByLemmaInAndSite_Id(wordCountMap.keySet(), siteModel.getId())
                        .parallelStream()
                        .peek(lemmaModel -> lemmaModel.setFrequency(lemmaModel.getFrequency() + wordCountMap.get(lemmaModel.getLemma())))
                        .collect(Collectors.toSet());

        wordCountMap.entrySet().removeIf(entry -> existingLemmaModels.parallelStream().map(LemmaModel::getLemma).toList().contains(entry.getKey()));

        existingLemmaModels.addAll(wordCountMap.entrySet().stream()
                .map(entry -> createLemmaModel(siteModel, entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet())
        );

        return existingLemmaModels;
    }

    /**
     * Retrieves the IndexModel list from the content of a PageModel.
     *
     * @param  pageModel    the PageModel to retrieve indexes from
     * @param  lemmas       the set of LemmaModels to search for in the content
     * @param  wordCountMap a map of word frequencies in the content
     * @return the list of IndexModels generated from the content
     */
    public Collection<IndexModel> getIndexModelFromContent(PageModel pageModel, Collection<LemmaModel> lemmas, Map<String, Integer> wordCountMap) {
        Set<IndexModel> existingIndexModels = indexRepository.findByPage_IdAndLemmaIn(pageModel.getId(), lemmas)
                .parallelStream()
                .peek(indexModel -> indexModel.setRank(indexModel.getRank() + wordCountMap.get(indexModel.getLemma())))
                .collect(Collectors.toSet());

        wordCountMap.entrySet().removeIf(entry-> existingIndexModels.parallelStream()
                .map(IndexModel::getLemma)
                .toList()
                .contains(lemmas.stream()
                        .filter(lemma -> lemma.getLemma().equals(entry.getKey()))
                        .findFirst()
                        .orElse(null)));

        existingIndexModels.addAll(wordCountMap.entrySet().parallelStream()
                .map(word2Count -> {
                        try {
                           LemmaModel lemmaModel = lemmas.stream().filter(lemma -> lemma.getLemma().equals(word2Count.getKey())).findFirst().get();
                           return createIndexModel(pageModel, lemmaModel, (float) word2Count.getValue());
                        } catch (StoppedExecutionException e) {
                            throw new RuntimeException(e.getLocalizedMessage());
                        }
                    })
                .collect(Collectors.toSet())
        );

        return existingIndexModels;
    }
    /**
     * Saves a set of entities to the database using the provided JpaRepository.
     * If an exception occurs during the save operation, the entities are saved individually
     * using the respective repository merge methods.
     *
     * @param entities the set of entities to save
     * @param repository the JpaRepository used to save the entities
     */
    public void saveEntities(Collection<?> entities, JpaRepository repository) {
        try {
            repository.saveAllAndFlush(entities);
        } catch (Exception e) {
            entities.forEach(entity -> {
                Class<?> aClass = entity.getClass();
                switch (aClass.getSimpleName()) {
                    case "PageModel":
                        PageModel pageModel = (PageModel) entity;
                        pageRepository.merge(pageModel.getId(), pageModel.getCode(), pageModel.getSite().getId(), pageModel.getContent(), pageModel.getPath(), pageModel.getVersion());
                        break;
                    case "LemmaModel":
                        LemmaModel lemmaModel = (LemmaModel) entity;
                        lemmaRepository.merge(lemmaModel.getLemma(), lemmaModel.getSite().getId(), lemmaModel.getFrequency());
                        break;
                    case "IndexModel":
                        IndexModel indexModel = (IndexModel) entity;
                        indexRepository.merge(indexModel.getLemma().getLemma(), indexModel.getPage().getId(), indexModel.getRank());
                        break;
                }
            });
        }
    }
    /**
     * Creates a new SiteModel object with the provided site information.
     *
     * @param  site  the Site object to create the SiteModel from
     * @return       the newly created SiteModel object
     */
    private SiteModel createSiteModel(Site site) {
        return SiteModel.builder()
                .status(Status.INDEXING)
                .url(site.getUrl())
                .statusTime(new Date())
                .lastError("")
                .name(site.getName())
                .build();
    }
    /**
     * Creates a new PageModel object with the provided siteModel and path.
     *
     * @param  siteModel  the SiteModel for the PageModel
     * @param  path       the path of the page
     * @return             the newly created PageModel object
     */
    private PageModel createPageModel(SiteModel siteModel, String path){
        ConnectionResponse connectionResponse = connection.getConnectionResponse(path);
        return PageModel.builder()
                .site(siteModel)
                .path(path)
                .code(connectionResponse.getResponseCode())
                .content(connectionResponse.getContent())
                .build();
    }
    /**
     * Creates a new LemmaModel object with the provided siteModel, lemma, and frequency.
     *
     * @param  siteModel   the SiteModel for the LemmaModel
     * @param  lemma       the lemma for the LemmaModel
     * @param  frequency   the frequency for the LemmaModel
     * @return             the newly created LemmaModel object
     */
    private LemmaModel createLemmaModel(SiteModel siteModel, String lemma, int frequency){
        return LemmaModel.builder()
                .site(siteModel)
                .lemma(lemma)
                .frequency(frequency)
                .build();
    }
    /**
     * Creates an IndexModel object with the given PageModel, LemmaModel, and ranking.
     *
     * @param  pageModel   the PageModel to associate with the IndexModel
     * @param  lemmaModel   the LemmaModel to associate with the IndexModel
     * @param  ranking     the ranking value to associate with the IndexModel
     * @return             the newly created IndexModel object
     */
    private IndexModel createIndexModel(PageModel pageModel, LemmaModel lemmaModel, Float ranking){
        return IndexModel.builder()
                .page(pageModel)
                .lemma(lemmaModel)
                .rank(ranking)
                .build();
    }
}