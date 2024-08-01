package searchengine.utils.entityHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.Site;
import searchengine.exceptions.StoppedExecutionException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.entityFactory.EntityFactory;
import searchengine.utils.morphology.Morphology;

import java.util.*;
import java.util.stream.Collectors;

import static searchengine.services.indexing.IndexingImpl.isIndexing;

/**
 * Util that handle and process kind of entities
 *
 * @author Ozsfag
 */
@Component
@RequiredArgsConstructor
public class EntityHandler {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    public final Morphology morphology;
    private final PageRepository pageRepository;
    private final EntityFactory entityFactory;
    private final LemmaHandler lemmaHandler;
    private final IndexHandler indexHandler;


    /**
     * Retrieves a collection of indexed SiteModel objects from a given collection of Site objects.
     *
     * @param  sitesToParse  the collection of Site objects to parse
     * @return                a collection of indexed SiteModel objects
     */
    public Collection<SiteModel> getIndexedSiteModelFromSites(Collection<Site> sitesToParse) {
        return sitesToParse.stream().map(site -> Optional.ofNullable(siteRepository.findSiteByUrl(site.getUrl()))
                .orElseGet(()-> entityFactory.createSiteModel(site))).collect(Collectors.toList());
    }


    /**
     * Indexes the lemmas and indexes for a list of pages.
     *
     * @param urlsToParse the list of pages to index
     * @param siteModel
     */
    public void processIndexing(Collection<String> urlsToParse, SiteModel siteModel) {
        Collection<PageModel> pages = getIndexedPageModelsFromUrls(urlsToParse, siteModel);
        saveEntities(pages);

        pages.forEach(page -> {
            Map<String, Integer> wordsCount = morphology.wordCounter(page.getContent());
            Collection<LemmaModel> lemmas = lemmaHandler.getIndexedLemmaModelsFromCountedWords(siteModel, wordsCount);
            saveEntities(lemmas);
            Collection<IndexModel> indexes = indexHandler.getIndexedIndexModelFromCountedWords(page, lemmas);
            saveEntities(indexes);
            siteRepository.updateStatusTimeByUrl(new Date(), siteModel.getUrl());
        });
    }

    /**
     * Retrieves the indexed PageModel list from the list of URLs.
     *
     * @param urlsToParse the list of URLs to parse
     * @param siteModel   the SiteModel containing the content
     * @return the set of indexed PageModels
     */
    public Set<PageModel> getIndexedPageModelsFromUrls(Collection<String> urlsToParse, SiteModel siteModel) {
        return urlsToParse.parallelStream()
                .map(url -> {
                    PageModel pageModel = entityFactory.createPageModel(siteModel, url);
                    if (!isIndexing) {
                        throw new StoppedExecutionException("Индексация остановлена пользователем");
                    }
                    return pageModel;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }



    /**
     * Saves a set of entities to the database using the provided JpaRepository.
     * If an exception occurs during the save operation, the entities are saved individually
     * using the respective repository merge methods.
     *
     * @param entities the set of entities to save
     */
    public void saveEntities(Collection<?> entities) {
        try {
            Class<?> repositoryClass = entities.iterator().next().getClass();
            switch (repositoryClass.getSimpleName()) {
                case "SiteModel":
                    Collection<SiteModel> sites = (Collection<SiteModel>) entities;
                    siteRepository.saveAll(sites);
                    break;
                case "PageModel":
                    Collection<PageModel> pages = (Collection<PageModel>) entities;
                    pageRepository.saveAll(pages);
                    break;
                case "LemmaModel":
                    Collection<LemmaModel> lemmas = (Collection<LemmaModel>) entities;
                    lemmaRepository.saveAll(lemmas);
                    break;
                case "IndexModel":
                    Collection<IndexModel> indexes = (Collection<IndexModel>) entities;
                    indexRepository.saveAll(indexes);
                    break;
            }
        } catch (Exception e) {
            entities.forEach(entity -> {
                Class<?> aClass = entity.getClass();
                switch (aClass.getSimpleName()) {
                    case "SiteModel":
                        SiteModel siteModel = (SiteModel) entity;
                        siteRepository.saveAndFlush(siteModel);
                        break;
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
}