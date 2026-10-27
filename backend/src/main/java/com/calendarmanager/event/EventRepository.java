package com.calendarmanager.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

	/** Rows of [categoryId, eventCount]. */
	@Query("select e.category.id, count(e) from Event e group by e.category.id")
	List<Object[]> countPerCategory();

	long countByCategory_Id(Long categoryId);

	long countByCategory_IdIn(Collection<Long> categoryIds);

	/** Events of the given calendars overlapping the half-open interval [from, to). */
	@Query("""
			select e from Event e
			where e.category.id in :categoryIds and e.endTime > :from and e.startTime < :to
			order by e.startTime, e.endTime desc, e.id""")
	List<Event> findOverlapping(@Param("categoryIds") Collection<Long> categoryIds, @Param("from") Instant from,
			@Param("to") Instant to);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Event e where e.category.id in :categoryIds")
	int deleteByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

}
