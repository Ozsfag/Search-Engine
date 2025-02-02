package searchengine.utils.indexing.executor.impl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.configuration.SitesList;
import searchengine.handlers.SiteIndexingHandler;
import searchengine.models.SiteModel;
import searchengine.utils.dataTransformer.DataTransformer;
import searchengine.utils.entitySaver.EntitySaverTemplate;
import searchengine.utils.indexing.executor.Executor;
import searchengine.utils.indexing.processor.Processor;

@Component
@RequiredArgsConstructor
public class ExecutorImpl implements Executor {
  private final EntitySaverTemplate entitySaverTemplate;
  private final SiteIndexingHandler siteIndexingHandler;
  private final Processor processor;
  private final SitesList sitesList;
  private final ForkJoinPool forkJoinPool;
  private final DataTransformer dataTransformer;

  @Override
  public void executeSeveralPagesIndexing() {
    Collection<CompletableFuture<Void>> futures = getFuturesForSiteModels();
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    CompletableFuture<List<Void>> allCompletableFuture =
        allFutures.thenApply(future -> futures.stream().map(CompletableFuture::join).toList());
    allCompletableFuture.toCompletableFuture();
  }

  private Collection<CompletableFuture<Void>> getFuturesForSiteModels() {
    Collection<SiteModel> siteModels = getSiteModels();
    siteModels = entitySaverTemplate.saveEntities(siteModels);

    return siteModels.parallelStream().map(this::getFutureProcess).toList();
  }

  private Collection<SiteModel> getSiteModels() {
    return siteIndexingHandler.getIndexedSiteModelFromSites(sitesList.getSites());
  }

  private CompletableFuture<Void> getFutureProcess(SiteModel siteModel) {
    return CompletableFuture.runAsync(
        () -> processor.processSiteIndexingRecursively(siteModel), forkJoinPool);
  }

  @Override
  public void executeOnePageIndexing(String url) {
    SiteModel siteModel = dataTransformer.transformUrlToSiteModel(url);
    Collection<String> urls = dataTransformer.transformUrlToUrls(url);
    processor.processOneSiteIndexing(url, siteModel, urls);
  }
}
