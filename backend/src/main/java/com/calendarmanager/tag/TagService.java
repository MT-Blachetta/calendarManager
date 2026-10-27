package com.calendarmanager.tag;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.calendarmanager.common.ConflictException;
import com.calendarmanager.common.NotFoundException;

@Service
@Transactional
public class TagService {

	static final String DEFAULT_COLOR = "#64748B";

	private final TagRepository tags;

	TagService(TagRepository tags) {
		this.tags = tags;
	}

	@Transactional(readOnly = true)
	public List<TagDto> findAll() {
		Map<Long, Long> usage = usageCounts();
		return tags.findAllByOrderByNameAsc().stream().map(t -> TagDto.of(t, usage.getOrDefault(t.getId(), 0L))).toList();
	}

	@Transactional(readOnly = true)
	public TagDto findById(Long id) {
		return TagDto.of(get(id), usageCounts().getOrDefault(id, 0L));
	}

	public TagDto create(TagRequest request) {
		String name = request.name().trim();
		if (tags.existsByNameIgnoreCase(name)) {
			throw new ConflictException("error.tag.duplicate", name);
		}
		Tag tag = tags.save(new Tag(name, colorOrDefault(request.color()), request.description()));
		return TagDto.of(tag, 0);
	}

	public TagDto update(Long id, TagRequest request) {
		Tag tag = get(id);
		tag.checkVersion(request.version());
		String name = request.name().trim();
		if (tags.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("error.tag.duplicate", name);
		}
		tag.setName(name);
		tag.setColor(colorOrDefault(request.color()));
		tag.setDescription(request.description());
		tags.flush();
		return TagDto.of(tag, usageCounts().getOrDefault(id, 0L));
	}

	public void delete(Long id) {
		get(id);
		tags.deleteTagById(id);
	}

	public Tag get(Long id) {
		return tags.findById(id).orElseThrow(() -> new NotFoundException("tag", id));
	}

	/** Loads all given tags or fails with 404 naming the first missing id. */
	public Set<Tag> getAll(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new HashSet<>();
		}
		Set<Long> wanted = new HashSet<>(ids);
		List<Tag> found = tags.findAllById(wanted);
		if (found.size() != wanted.size()) {
			Set<Long> missing = new HashSet<>(wanted);
			found.forEach(t -> missing.remove(t.getId()));
			throw new NotFoundException("tag", missing.iterator().next());
		}
		return new HashSet<>(found);
	}

	private Map<Long, Long> usageCounts() {
		Map<Long, Long> result = new HashMap<>();
		for (Object[] row : tags.countDirectUsage()) {
			result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		}
		return result;
	}

	private static String colorOrDefault(String color) {
		return color == null || color.isBlank() ? DEFAULT_COLOR : color.toUpperCase();
	}

}
