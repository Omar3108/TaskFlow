package com.taskflow.card;

import com.taskflow.label.Label;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the query of the board view from the optional filters. A JPA Specification is one
 * predicate; they are combined with AND, so each filter is written once and only the ones the
 * client sent are added. The alternative, a single JPQL string with "(:x is null or ...)" for
 * every filter, grows harder to read with each filter and gives the database a worse plan.
 */
public final class CardSpecifications {

    private static final char ESCAPE = '\\';

    private CardSpecifications() {
    }

    public static Specification<Card> forBoard(UUID boardId, CardFilter filter, LocalDate today) {
        List<Specification<Card>> specs = new ArrayList<>();
        specs.add(inBoard(boardId));
        specs.add(fetchAssignee());
        if (filter.q() != null && !filter.q().isBlank()) {
            specs.add(containsText(filter.q().trim()));
        }
        if (filter.assigneeId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("assignee").get("id"), filter.assigneeId()));
        }
        if (Boolean.TRUE.equals(filter.unassigned())) {
            specs.add((root, query, cb) -> cb.isNull(root.get("assignee")));
        }
        if (filter.labelId() != null) {
            specs.add(hasLabel(filter.labelId()));
        }
        if (filter.due() != null) {
            specs.add(due(filter.due(), today));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Card> inBoard(UUID boardId) {
        return (root, query, cb) -> cb.equal(root.get("column").get("board").get("id"), boardId);
    }

    /** Loads the assignee in the same query (the response shows it), avoiding N+1 queries. */
    private static Specification<Card> fetchAssignee() {
        return (root, query, cb) -> {
            // A count query (used by paging) must not fetch: there is no entity to fill.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("assignee", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    /** Case-insensitive "contains" on title or description: LOWER(x) LIKE '%text%'. */
    private static Specification<Card> containsText(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
                // LIKE on a NULL description is NULL, i.e. "no match", which is what we want.
                cb.like(cb.lower(root.get("description")), pattern, ESCAPE));
    }

    /**
     * EXISTS (card has the label) instead of joining the labels: a join would repeat a card
     * once per matching label and would need DISTINCT, which clashes with ORDER BY and fetch.
     */
    private static Specification<Card> hasLabel(UUID labelId) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Card> card = subquery.from(Card.class);
            Join<Card, Label> label = card.join("labels");
            subquery.select(card.get("id"))
                    .where(cb.equal(card.get("id"), root.get("id")), cb.equal(label.get("id"), labelId));
            return cb.exists(subquery);
        };
    }

    private static Specification<Card> due(DueFilter due, LocalDate today) {
        return (root, query, cb) -> switch (due) {
            case OVERDUE -> cb.lessThan(root.get("dueDate"), today);
            case WEEK -> cb.between(root.get("dueDate"), today, today.plusDays(7));
            case NONE -> cb.isNull(root.get("dueDate"));
        };
    }

    /**
     * The user types plain text: "%" or "_" must match themselves, not act as wildcards. Each
     * special character (and the escape character itself) is prefixed with the escape.
     */
    static String escapeLike(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
