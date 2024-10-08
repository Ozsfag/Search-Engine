package searchengine.repositories;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageModel;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

  @Transactional(readOnly = true)
  @Query("SELECT p.path FROM PageModel p WHERE p.path IN :paths")
  Set<String> findAllPathsByPathIn(@Param("paths") @NonNull Collection<String> paths);

  @Transactional
  @Modifying
  @Query(
      "UPDATE PageModel p SET p.code = :code, p.site = :siteId, p.content = :content, p.path = :path WHERE p.id = :id")
  void merge(
      @Param("id") Integer id,
      @Param("code") Integer code,
      @Param("siteId") Integer siteId,
      @Param("content") String content,
      @Param("path") String path);

  @Transactional(readOnly = true)
  @Query(
      "SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM PageModel p WHERE p.path = :path")
  boolean existsByPath(@Param("path") String path);
}
