package com.sismics.docs.core.dao;

import com.sismics.docs.core.model.jpa.Registration;
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Registration DAO.
 */
public class RegistrationDao {
    /**
     * Creates a new registration.
     *
     * @param registration Registration
     * @return New ID
     */
    public String create(Registration registration) {
        // Create the UUID
        registration.setId(UUID.randomUUID().toString());

        // Set creation date
        registration.setCreateDate(new Date());

        // Default status is PENDING
        registration.setStatus(Registration.Status.PENDING);

        // Create the registration
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(registration);

        return registration.getId();
    }

    /**
     * Get a registration by ID.
     *
     * @param id Registration ID
     * @return Registration
     */
    public Registration getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return em.find(Registration.class, id);
    }

    /**
     * Search registrations by status.
     *
     * @param status Status to search for
     * @return List of registrations
     */
    @SuppressWarnings("unchecked")
    public List<Registration> getByStatus(Registration.Status status) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select r from Registration r where r.status = :status order by r.createDate");
        q.setParameter("status", status);
        return q.getResultList();
    }

    /**
     * Update a registration's status.
     *
     * @param id        Registration ID
     * @param newStatus New status
     */
    public void updateStatus(String id, Registration.Status newStatus) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Registration registration = getById(id);
        if (registration != null) {
            // Update the status
            Query q = em.createQuery("update Registration r set r.status = :status where r.id = :id");
            q.setParameter("status", newStatus);
            q.setParameter("id", id);
            q.executeUpdate();
        }
    }

    /**
     * Delete a registration.
     *
     * @param id Registration ID
     */
    public void delete(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();

        Registration registration = getById(id);
        if (registration != null) {
            // Delete the registration
            Query q = em.createQuery("delete from Registration r where r.id = :id");
            q.setParameter("id", id);
            q.executeUpdate();
        }
    }

    /**
     * Check if a username already exists in pending registrations.
     *
     * @param username Username to check
     * @return True if username already exists in pending registrations
     */
    public boolean isUsernameExists(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery(
                "select count(r) from Registration r where r.status = :status and r.username = :username");
        q.setParameter("status", Registration.Status.PENDING);
        q.setParameter("username", username);
        return ((Long) q.getSingleResult()) > 0;
    }

    /**
     * Check if an email already exists in pending registrations.
     *
     * @param email Email to check
     * @return True if email already exists in pending registrations
     */
    public boolean isEmailExists(String email) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery(
                "select count(r) from Registration r where r.status = :status and r.email = :email");
        q.setParameter("status", Registration.Status.PENDING);
        q.setParameter("email", email);
        return ((Long) q.getSingleResult()) > 0;
    }
}