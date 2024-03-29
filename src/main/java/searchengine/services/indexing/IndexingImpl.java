package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import searchengine.config.MorphologySettings;
import searchengine.config.SitesList;
import searchengine.dto.ResponseInterface;
import searchengine.dto.indexing.responseImpl.Bad;
import searchengine.dto.indexing.responseImpl.Stop;
import searchengine.dto.indexing.responseImpl.Successful;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.connectivity.Connection;
import searchengine.utils.entityHandler.EntityHandler;
import searchengine.utils.morphology.Morphology;
import searchengine.utils.parser.Parser;

import java.util.Date;
import java.util.List;
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
    private final ForkJoinPool forkJoinPool;
    @Lazy
    private final Morphology morphology;
    private final EntityHandler entityHandler;
    private final Connection connection;
    private final MorphologySettings morphologySettings;
    public static volatile boolean isIndexing = true;
    public static final Logger logger = LoggerFactory.getLogger(IndexingImpl.class);
    @Override
    public ResponseInterface startIndexing() {
        logger.debug("start indexing");
        if (!isIndexing) return new Bad(false, "Индексация уже запущена");
        CompletableFuture.runAsync(() ->
                sitesList.getSites()
                        .parallelStream()
                        .forEach(siteUrl -> processSite(siteUrl.getUrl())), forkJoinPool)
                .thenAccept(item -> forkJoinPool.shutdown());
        logger.debug("all pages indexed successfully");
        return new Successful(true);
    }
    @SneakyThrows
    private Object processSite(String siteUrl) {
        return forkJoinPool.submit(() -> {
            logger.debug("begining process Site: " + siteUrl);
            try {
                SiteModel siteModel = entityHandler.getIndexedSiteModel(siteUrl);
                Parser parser = new Parser(entityHandler, connection, morphology, siteModel, siteUrl, pageRepository, morphologySettings);
                forkJoinPool.invoke(parser);
                siteRepository.updateStatusAndStatusTimeByUrl(Status.INDEXED, new Date(), siteUrl);
                logger.debug("Site: " + siteUrl + " indexed successfully");
            } catch (Exception re) {
                siteRepository.updateStatusAndStatusTimeAndLastErrorByUrl(Status.FAILED, new Date(), re.getLocalizedMessage(), siteUrl);
                logger.debug("indexing complete with error");
            }
        }).get();
    }
    @Override
    public ResponseInterface stopIndexing() {
        if (!isIndexing) return new Stop(false, "Индексация не запущена");
        isIndexing = false;
        logger.debug("indexing stopped");
        return new Stop(true, "Индексация остановлена пользователем");
    }
    @SneakyThrows
    @Override
    public ResponseInterface indexPage(String url) {
        logger.debug("start indexing single page");
        if (isIndexing) {
            return new Bad(false, "Индексация не может быть начата во время другого процесса индексации");
        }
        SiteModel siteModel = entityHandler.getIndexedSiteModel(url);
        PageModel pageModel = entityHandler.getPageModel(siteModel, url);
        pageRepository.saveAndFlush(pageModel);
        List<LemmaModel> lemmas = entityHandler.getIndexedLemmaModelListFromContent(pageModel, siteModel);
        entityHandler.getIndexModelFromContent(pageModel, siteModel, lemmas);
        logger.debug("end indexing single page");
        return new Successful(true);
    }

}
