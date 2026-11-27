package com.vishesh.foodcatalog.catalog.persistence.entity;

import java.time.Instant;
import java.util.Currency;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vishesh.foodcatalog.catalog.domain.ChainStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** The database-shaped view of one physical outlet. */
@Entity
@Table(name = "outlet")
public class OutletEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "outlet_code", nullable = false, length = 100)
	private String outletCode;

	@Column(name = "restaurant_chain_id", nullable = false)
	private Long restaurantChainId;

	// Flyway deliberately uses CHAR(3): ISO 4217 currency codes always have
	// exactly three characters (for example INR or EUR). Tell Hibernate that
	// this is a SQL CHAR, not its default VARCHAR mapping for String.
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "currency", nullable = false, length = 3)
	private String currencyCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ChainStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "timezone", nullable = false)
	private String timezone;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected OutletEntity() {
		// JPA uses this constructor while rebuilding an outlet from a database row.
	}

	public OutletEntity(Long id, Long restaurantChainId, String displayName, String outletCode, Currency currency,
			ChainStatus status, Instant createdAt, Instant updatedAt, String timezone, Long version) {
		this.id = id;
		this.restaurantChainId = restaurantChainId;
		this.displayName = displayName;
		this.outletCode = outletCode;
		this.currencyCode = currency == null ? null : currency.getCurrencyCode();
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.timezone = timezone;
		this.version = version;
	}

	public Long getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getOutletCode() {
		return outletCode;
	}

	public Long getRestaurantChainId() {
		return restaurantChainId;
	}

	public Currency getCurrency() {
		return Currency.getInstance(currencyCode);
	}

	public ChainStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getTimezone() {
		return timezone;
	}

	public Long getVersion() {
		return version;
	}

}
