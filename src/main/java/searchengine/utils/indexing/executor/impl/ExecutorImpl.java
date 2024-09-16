package searchengine.utils.indexing.executor.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.utils.entityHandlers.SiteHandler;
import searchengine.utils.entitySaver.strategy.EntitySaverStrategy;
import searchengine.utils.indexing.executor.Executor;
import searchengine.utils.indexing.processor.Processor;

@Component
@RequiredArgsConstructor
public class ExecutorImpl implements Executor {
  private final EntitySaverStrategy entitySaverStrategy;
  private final SiteHandler siteHandler;
  private final Processor processor;
  private final SitesList sitesList;

  @Override
  public void executeIndexing() {
    Collection<CompletableFuture<Void>> futures = getFuturesForSiteModels();
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  private Collection<CompletableFuture<Void>> getFuturesForSiteModels() {
    Collection<SiteModel> siteModels = getSiteModels();
    entitySaverStrategy.saveEntities(siteModels);

    return siteModels.stream().map(this::getFutureProcess).toList();
  }

  private Collection<SiteModel> getSiteModels() {
    return siteHandler.getIndexedSiteModelFromSites(sitesList.getSites());
  }

  private CompletableFuture<Void> getFutureProcess(SiteModel siteModel) {
    return CompletableFuture.runAsync(() -> processor.processSiteIndexing(siteModel));
  }
}
