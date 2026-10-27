package com.calendarmanager.common;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

/**
 * Common columns of all persistent entities: identity id, optimistic-locking version and audit timestamps.
 */
@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Optimistic-locking check for update requests that carry the version the client last saw.
	 */
	public void checkVersion(Long expectedVersion) {
		if (expectedVersion != null && expectedVersion != version) {
			String type = getClass().getSimpleName();
			throw new ConflictException("error.versionConflict",
					NotFoundException.resourceName(Character.toLowerCase(type.charAt(0)) + type.substring(1)),
					String.valueOf(id), version, expectedVersion);
		}
	}

}
