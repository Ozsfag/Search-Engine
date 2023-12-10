package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import searchengine.model.SiteModel;
import searchengine.services.connectivity.ConnectionService;
import searchengine.services.entityHandler.EntityHandlerService;
import searchengine.services.morphology.LemmaFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class Parser extends RecursiveAction {
    private final EntityHandlerService entityHandlerService;
    private final ConnectionService connectionService;
    private final LemmaFinder lemmaFinder;
    private final SiteModel siteModel;
    private final String href;

    @Override
    protected  void compute() {
        List<Parser> taskQueue = new ArrayList<>();

        connectionService.getConnection(href).getUrls().stream()
                .filter(url -> url.absUrl("href").startsWith(siteModel.getUrl()))
                .map(element -> element.absUrl("href"))
                .map(href -> entityHandlerService.getIndexedPageModel(siteModel, href))
                .forEach(page -> {
                    lemmaFinder.handleLemmaModel(siteModel, page);
                    taskQueue.add(new Parser(entityHandlerService, connectionService, lemmaFinder, siteModel, page.getPath()));
                });
        taskQueue.forEach(RecursiveAction::fork);
        taskQueue.forEach(RecursiveAction::join);
    }
}
