package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import searchengine.config.MorphologySettings;
import searchengine.config.SitesList;
import searchengine.dto.ResponseInterface;
import searchengine.dto.indexing.responseImpl.Bad;
import searchengine.dto.indexing.responseImpl.Stop;
import searchengine.dto.indexing.responseImpl.Successful;
import searchengine.exceptions.StoppedExecutionException;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.connectivity.Connection;
import searchengine.utils.entityHandler.EntityHandler;
import searchengine.utils.morphology.Morphology;
import searchengine.utils.parser.Parser;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingImpl implements IndexingService {
    private final SitesList sitesList;
    @Lazy
    private final SiteRepository siteRepository;
    @Lazy
    private final PageRepository pageRepository;
    @Lazy
    private final ForkJoinPool forkJoinPool;
    @Lazy
    private final Morphology morphology;
    @Lazy
    private final EntityHandler entityHandler;
    @Lazy
    private final Connection connection;
    @Lazy
    private final MorphologySettings morphologySettings;
    @Lazy
    private final LemmaRepository lemmaRepository;
    @Lazy
    private final IndexRepository indexRepository;
    @Lazy
    public static volatile boolean isIndexing = true;
    /**
     * Starts the indexing process.
     *
     * @return A ResponseInterface indicating the success or failure of the indexing process.
     */
    @Override
    public ResponseInterface startIndexing() {
        if (!isIndexing) return new Bad(false, "Индексация уже запущена");
        CompletableFuture.runAsync(() ->
                sitesList.getSites()
                        .parallelStream()
                        .forEach(siteUrl -> processSite(siteUrl.getUrl())), forkJoinPool)
                .thenAccept(item -> forkJoinPool.shutdown());
        return new Successful(true);
    }
    /**
     * Processes a given site by submitting a task to a ForkJoinPool executor.
     * The task attempts to get the indexed site model for the given site URL,
     * creates a parser for the site model, invokes the parser using the ForkJoinPool,
     * and updates the status and status time of the site in the site repository.
     * If an exception occurs during the process, the status and status time of the site
     * are updated with the failed status and the localized error message.
     *
     * @param  siteUrl  the URL of the site to be processed
     * @return           an Object representing the result of the process
     */
    @SneakyThrows
    private Object processSite(String siteUrl) {
        return forkJoinPool.submit(() -> {
            try {
                SiteModel siteModel = entityHandler.getIndexedSiteModel(siteUrl);
                Parser parser = getParser(siteModel, siteUrl);
                forkJoinPool.invoke(parser);
                siteRepository.updateStatusAndStatusTimeByUrl(Status.INDEXED, new Date(), siteUrl);
            } catch (RuntimeException re) {
                siteRepository.updateStatusAndStatusTimeAndLastErrorByUrl(Status.FAILED, new Date(), re.getLocalizedMessage(), siteUrl);
            }
        }).get();
    }
    /**
     * Creates and returns a Parser object based on the provided parameters.
     *
     * @param  siteModel    the SiteModel object for the parser
     * @param  siteUrl      the URL of the site for the parser
     * @return              a new Parser object initialized with the given parameters
     */
    private Parser getParser(SiteModel siteModel, String siteUrl){
        return new Parser(
                entityHandler,
                connection,
                morphology,
                siteModel,
                siteUrl,
                pageRepository,
                morphologySettings,
                lemmaRepository,
                indexRepository,
                siteRepository);
    }
    /**
     * Stops the indexing process if it is currently running.
     *
     * @return          an object representing the result of stopping the indexing process
     */
    @Override
    public ResponseInterface stopIndexing() {
        if (!isIndexing) return new Stop(false, "Индексация не запущена");
        isIndexing = false;
        return new Stop(true, "Индексация остановлена пользователем");
    }
    /**
     * Indexes a single page.
     *
     * @param  url   the URL of the page to be indexed
     * @return       a ResponseInterface object indicating the success or failure of the indexing process
     */
    @SneakyThrows
    @Override
    public ResponseInterface indexPage(String url) {
        if (isIndexing) {
            return new Bad(false, "Индексация не может быть начата во время другого процесса индексации");
        }
        SiteModel siteModel = entityHandler.getIndexedSiteModel(url);
        PageModel pageModel = entityHandler.getPageModel(siteModel, url);
        pageRepository.saveAndFlush(pageModel);
        Map<String, Integer> wordCountMap = morphology.wordCounter(pageModel.getContent());
        Set<LemmaModel> lemmas = entityHandler.getIndexedLemmaModelListFromContent(siteModel, wordCountMap);
        entityHandler.getIndexModelFromContent(pageModel, lemmas, wordCountMap);
        return new Successful(true);
    }

}