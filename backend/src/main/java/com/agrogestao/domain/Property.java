package com.agrogestao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "properties", indexes = {
        @Index(name = "idx_properties_user_id", columnList = "user_id")
})
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String city;

    @Column(length = 2)
    private String state;

    @Column(name = "total_area_ha", precision = 19, scale = 4)
    private BigDecimal totalAreaHa;

    @Column(columnDefinition = "TEXT")
    private String description;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public BigDecimal getTotalAreaHa() {
        return totalAreaHa;
    }

    public void setTotalAreaHa(BigDecimal totalAreaHa) {
        this.totalAreaHa = totalAreaHa;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
