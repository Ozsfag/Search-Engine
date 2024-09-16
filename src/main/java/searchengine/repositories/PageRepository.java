package searchengine.repositories;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
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

  @Transactional()
  @Query("SELECT p.path FROM PageModel p WHERE p.site.id = :siteId AND p.path IN :paths")
  Set<String> findAllPathsBySiteAndPathIn(
      @Param("siteId") int siteId, @Param("paths") @NonNull Collection<String> paths);
  https://habr.com/ru/articles/567368/
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

  @Transactional
  @Query("select (count(p) > 0) from PageModel p where p.path = ?1")
  boolean existsByPath(String path);
}
