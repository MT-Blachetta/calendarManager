package com.calendarmanager.tag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<Tag> findAllByOrderByNameAsc();

	/**
	 * Deletes a tag; category and view-filter references are removed by ON DELETE CASCADE, so the persistence
	 * context is cleared afterwards.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Tag t where t.id = :id")
	int deleteTagById(@Param("id") Long id);

	/** Number of categories each tag is directly attached to: rows of [tagId, count]. */
	@Query(value = "select tag_id, count(*) from category_tag group by tag_id", nativeQuery = true)
	List<Object[]> countDirectUsage();

}
