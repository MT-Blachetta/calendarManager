package com.calendarmanager.view;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarViewRepository extends JpaRepository<CalendarView, Long> {

	List<CalendarView> findAllByOrderBySystemDescSortOrderAscNameAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
