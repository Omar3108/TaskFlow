package com.taskflow.card;

import com.taskflow.board.BoardColumn;
import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import com.taskflow.label.Label;

@Entity
@Table(name = "cards")
// Only write the columns that changed. Otherwise an edit loaded before a move would write its
// stale position back, undoing the move (the version does not move, so nothing would catch it).
@DynamicUpdate
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Not "updatable = false": moving a card to another column changes this.
    // Excluded from the version: a move is not a content edit, so it must not make the copy of
    // the card that somebody else has open stale (see CardService.move).
    @OptimisticLock(excluded = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @OptimisticLock(excluded = true) // same reason as column: moving is not an edit
    @Column(nullable = false)
    private double position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    // Card is the owning side: Hibernate writes card_labels when this set changes. Label has no
    // collection back, because nothing needs to navigate from a label to its cards.
    // Changing the set is a content edit, so it bumps the version (collections take part in
    // optimistic locking by default), and it only happens through the version-checked update.
    @ManyToMany
    @JoinTable(
            name = "card_labels",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private Set<Label> labels = new HashSet<>();

    // Optimistic locking: Hibernate adds "AND version = ?" to every UPDATE/DELETE, increments the
    // value on each update and throws when no row matched. No setter: only Hibernate writes it.
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Derived from the other fields, so touching it alone must not bump the version either.
    @OptimisticLock(excluded = true)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Card() {
        // Required by JPA.
    }

    public Card(BoardColumn column, String title, double position, User createdBy) {
        this.column = column;
        this.title = title;
        this.position = position;
        this.createdBy = createdBy;
    }

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

    public UUID getId() {
        return id;
    }

    public BoardColumn getColumn() {
        return column;
    }

    public void setColumn(BoardColumn column) {
        this.column = column;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    /** Replaces the labels in place: Hibernate tracks the managed collection, not the field. */
    public void setLabels(Set<Label> newLabels) {
        labels.retainAll(newLabels);
        labels.addAll(newLabels);
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
}
