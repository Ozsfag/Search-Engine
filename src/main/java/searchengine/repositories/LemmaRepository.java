package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaModel;

import javax.persistence.LockModeType;


@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT l FROM LemmaModel l WHERE l.lemma = :lemma AND l.site.id = :siteId")
    LemmaModel findByLemmaAndSite_Id(@Param("lemma") String lemma, @Param("siteId") int siteId);
    int countBySite_Url(String url);
}
