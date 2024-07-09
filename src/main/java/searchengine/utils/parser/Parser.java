package searchengine.utils.parser;

import lombok.RequiredArgsConstructor;
import searchengine.config.MorphologySettings;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.Adder;
import searchengine.utils.connectivity.Connection;
import searchengine.utils.entityHandler.EntityHandler;
import searchengine.utils.morphology.Morphology;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Recursively index page and it`s subpage.
 * @author Ozsfag
 */
@RequiredArgsConstructor
public class Parser extends RecursiveTask<Void> {
    private final EntityHandler entityHandler;
    private final Connection connection;
    private final Morphology morphology;
    private final SiteModel siteModel;
    private final String href;
    private final PageRepository pageRepository;
    private final MorphologySettings morphologySettings;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final Adder adder;

    /**
     * Recursively computes the parsing of URLs and initiates subtasks for each URL to be parsed.
     *
     * @return         	null
     */
    @Override
    protected Void compute() {
        Set<String> urlsToParse = getUrlsToParse();
        if (!urlsToParse.isEmpty()) {
            indexingProcess(urlsToParse);
            siteRepository.updateStatusTimeByUrl(new Date(), siteModel.getUrl());
            List<Parser> subtasks = urlsToParse.stream()
                    .map(url -> new Parser(
                            entityHandler,
                            connection,
                            morphology,
                            siteModel,
                            url,
                            pageRepository,
                            morphologySettings,
                            lemmaRepository,
                            indexRepository,
                            siteRepository,
                            adder)
                    )
                    .toList();
            invokeAll(subtasks);
        }
        return null;
    }

    /**
     * Retrieves a list of PageModel objects from the provided list of URLs to parse.
     *
     * @param  urlsToParse  the list of URLs to parse
     * @return              a list of PageModel objects representing the pages parsed from the URLs
     */
    private Set<PageModel> getPages(Set<String> urlsToParse) {
        return urlsToParse.stream().parallel()
                .map(url -> {
                    try {
                        return entityHandler.getPageModel(siteModel, url);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                    }
                })
                .collect(Collectors.toSet());
    }
    /**
     * Retrieves a list of URLs to parse based on the provided list of all URLs by site.
     *
     * @return                 list of URLs to parse
     */
    private Set<String> getUrlsToParse() {
        CopyOnWriteArrayList<String> urls = (CopyOnWriteArrayList<String>) connection.getConnectionResponse(href).getUrls();
        CopyOnWriteArrayList<String> alreadyParsed = pageRepository.findAllPathsBySite(siteModel.getId());
        urls.removeAll(alreadyParsed);
        return urls.parallelStream()
                .filter(url -> url.startsWith(siteModel.getUrl()) &&
                        Arrays.stream(morphologySettings.getFormats()).noneMatch(url::contains))
                .collect(Collectors.toSet());
    }
    /**
     * Indexes the lemmas and indexes for a list of pages.
     *
     * @param  urlsToParse   the list of pages to index
     */
    private void indexingProcess(Set<String> urlsToParse) {
        Set<PageModel> pages = getPages(urlsToParse);

//        pageRepository.saveAllAndFlush(pages);
        pages.stream()
                .map(adder::updateOrInsertUsingBuiltInFeature)
                .forEach(page -> {
//                    pageRepository.saveAndFlush(page);
                    Map<String, Integer> wordCountMap = morphology.wordCounter(page.getContent());
                    Set<LemmaModel> lemmas = entityHandler.getIndexedLemmaModelListFromContent(siteModel, wordCountMap);
                    lemmaRepository.saveAllAndFlush(lemmas);
                    Set<IndexModel> indexes = entityHandler.getIndexModelFromContent(page, lemmas, wordCountMap);
                    indexRepository.saveAllAndFlush(indexes);
        });
    }
}
