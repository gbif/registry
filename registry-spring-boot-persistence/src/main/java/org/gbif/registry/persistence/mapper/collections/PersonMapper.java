package org.gbif.registry.persistence.mapper.collections;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for {@link Person} entities.
 */
@Repository
public interface PersonMapper extends CrudMapper<Person> {

  // TODO: 2019-07-24 get, create, delete, update inherited explicitly because of exception
  Person get(@Param("key") UUID key);

  void create(Person entity);

  void delete(@Param("key") UUID key);

  void update(Person entity);

  List<Person> list(@Nullable @Param("institutionKey") UUID institutionKey,
                    @Nullable @Param("collectionKey") UUID collectionKey,
                    @Nullable @Param("query") String query,
                    @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("institutionKey") UUID institutionKey,
             @Nullable @Param("collectionKey") UUID collectionKey,
             @Nullable @Param("query") String query);

  /**
   * A simple suggest by title service.
   */
  List<PersonSuggestResult> suggest(@Nullable @Param("q") String q);

  /**
   * @return the persons marked as deleted
   */
  List<Person> deleted(@Param("page") Pageable page);

  /**
   * @return the count of the persons marked as deleted.
   */
  long countDeleted();

}
