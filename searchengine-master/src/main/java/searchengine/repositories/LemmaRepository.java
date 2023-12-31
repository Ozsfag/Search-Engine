package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    LemmaModel findByLemma(String lemma);

    int countBySite_Url(String url);

}
