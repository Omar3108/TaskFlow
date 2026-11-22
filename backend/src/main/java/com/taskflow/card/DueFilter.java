package com.taskflow.card;

/** Due-date filter of the board view. Dates are compared in UTC (the application clock). */
public enum DueFilter {
    /** Due before today. */
    OVERDUE,
    /** Due today or in the next seven days. */
    WEEK,
    /** Without a due date. */
    NONE
}
