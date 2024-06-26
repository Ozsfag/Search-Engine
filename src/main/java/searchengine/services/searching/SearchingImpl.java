package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import searchengine.dto.ResponseInterface;
import searchengine.dto.searching.responseImpl.DetailedSearchResponse;
import searchengine.dto.searching.responseImpl.TotalEmptyResponse;
import searchengine.dto.searching.responseImpl.TotalSearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.connectivity.Connection;
import searchengine.utils.entityHandler.EntityHandler;
import searchengine.utils.morphology.Morphology;

import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Service
@Lazy
@RequiredArgsConstructor
public class SearchingImpl implements SearchingService {
    private final EntityHandler entityHandler;
    private final Morphology morphology;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final Connection connection;
    private static final int MAX_FREQUENCY = 5000;
    @Override
    public ResponseInterface search(String query, String site, int offset, int limit) {
        SiteModel siteModel = site == null ? null : entityHandler.getIndexedSiteModel(site);

        Set<IndexModel> uniqueSet = transformQueryToIndexModelSet(query, siteModel);
        if (uniqueSet.isEmpty()){return new TotalEmptyResponse(false, "Not found");}

        Map<Integer, Float> rel = getPageId2AbsRank(uniqueSet);

        List<DetailedSearchResponse> detailedSearchResponse = getDetailedSearchResponses(rel, offset, limit, uniqueSet);

        return new TotalSearchResponse(true, detailedSearchResponse.size(), detailedSearchResponse);
    }
    private List<DetailedSearchResponse> getDetailedSearchResponses(Map<Integer, Float> rel, int offset, int limit, Set<IndexModel> uniqueSet){
        return rel.entrySet().stream()
                .skip(offset)
                .limit(limit)
                .map(entry -> {
                    DetailedSearchResponse response = new DetailedSearchResponse();
                    try {
                        PageModel pageModel = pageRepository.findById(entry.getKey()).orElseThrow();
                        String[] urlComponents = morphology.getValidUrlComponents(pageModel.getPath());
                        response.setUri(urlComponents[1]);
                        response.setSite(urlComponents[0]);
                        response.setSiteName(pageModel.getSite().getName());
                        response.setRelevance(entry.getValue());
                        response.setTitle(connection.getTitle(pageModel.getPath()));
                        response.setSnippet(getSnippet(uniqueSet, pageModel));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                    }
                    return response;
                })
                .sorted(Comparator.comparing(DetailedSearchResponse::getRelevance))
                .collect(Collectors.toCollection(LinkedList::new));
    }
    private Set<IndexModel> transformQueryToIndexModelSet(String query, SiteModel siteModel) {
        return morphology.getLemmaSet(query).stream()
                .flatMap(queryWord -> siteModel == null ?
                        indexRepository.findIndexBy2Params(queryWord, MAX_FREQUENCY).stream() :
                        indexRepository.findIndexBy3Params(queryWord, MAX_FREQUENCY, siteModel).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

    }

    private Map<Integer, Float> getPageId2AbsRank(Set<IndexModel> uniqueSet){

        Map<Integer, Float> pageId2AbsRank = uniqueSet.stream()
                .collect(Collectors.toMap(index -> index.getPage().getId(),
                        IndexModel::getRank,
                        Float::sum,
                        HashMap::new));

        var maxValues = pageId2AbsRank.values().stream()
                .max(Float::compareTo)
                .orElse(1f);

        return pageId2AbsRank.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, id2AbsRank -> id2AbsRank.getValue() / maxValues));
    }
    public String getSnippet(Set<IndexModel> uniqueSet, PageModel pageModel) {
        List<String> matchingSentences = new ArrayList<>();
        uniqueSet.stream()
                .filter(item -> item.getPage().equals(pageModel))
                .forEach(item -> {
                    String content = pageModel.getContent().toLowerCase(Locale.ROOT);
                    String word = item.getLemma().getLemma();
                    Matcher matcher = Pattern.compile(Pattern.quote(word)).matcher(content);
                    String matchingSentence = null;
                    while (matcher.find()) {
                        int start = Math.max(matcher.start() - 100, 0);
                        int end = Math.min(matcher.end() + 100, content.length());
                        word = matcher.group();
                        matchingSentence = content.substring(start, end);
                        matchingSentence = matchingSentence.replaceAll(word, "<b>" + word + "</b>");
                    }
                    matchingSentences.add(matchingSentence);
                });
        return String.join("............. ", matchingSentences);
    }
}
