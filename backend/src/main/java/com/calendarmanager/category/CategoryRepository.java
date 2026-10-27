package com.calendarmanager.category;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	@Query("select distinct c from Category c left join fetch c.tags")
	List<Category> findAllWithTags();

	long countByKind(CategoryKind kind);

	@Query(value = "select count(distinct view_id) from view_category_filter where category_id in (:ids)",
			nativeQuery = true)
	long countViewsReferencing(@Param("ids") Collection<Long> ids);

	/**
	 * Deletes a whole subtree in one statement (parent FK is checked at statement end). Join rows (tags, view
	 * filters) are removed by ON DELETE CASCADE, so the persistence context is cleared afterwards.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Category c where c.id in :ids")
	int deleteAllByIdIn(@Param("ids") Collection<Long> ids);

}
