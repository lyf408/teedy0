package com.sismics.docs.core.model.jpa;

import com.google.common.base.MoreObjects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

/**
 * Registration entity representing user registration requests.
 */
@Entity
@Table(name = "T_REGISTRATION")
public class Registration {
    /**
     * Registration status enumeration.
     */
    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    /**
     * Identifier.
     */
    @Id
    @Column(name = "REG_ID_C", length = 36)
    private String id;

    /**
     * Username.
     */
    @Column(name = "REG_USERNAME_C", nullable = false, length = 50)
    private String username;

    /**
     * Email address.
     */
    @Column(name = "REG_EMAIL_C", nullable = false, length = 100)
    private String email;

    /**
     * Registration status.
     */
    @Column(name = "REG_STATUS_C", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Creation date.
     */
    @Column(name = "REG_CREATEDATE_D", nullable = false)
    private Date createDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("username", username)
                .add("email", email)
                .add("status", status)
                .toString();
    }
}