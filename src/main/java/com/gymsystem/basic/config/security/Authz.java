package com.gymsystem.basic.config.security;

/**
 * Expressions for @PreAuthorize. The permission codes are the rows of the `permission` table
 * and travel inside the access token.
 */
public final class Authz {
    private Authz() {
    }

    public static final String AUTHENTICATED = "isAuthenticated()";

    public static final String BRANCH_MANAGE = "hasAuthority('BRANCH_MANAGE')";
    public static final String STAFF_MANAGE = "hasAuthority('STAFF_MANAGE')";
    public static final String PERMISSION_GRANT = "hasAuthority('PERMISSION_GRANT')";
    public static final String MEMBER_MANAGE = "hasAuthority('MEMBER_MANAGE')";
    public static final String ATTENDANCE_RECORD = "hasAuthority('ATTENDANCE_RECORD')";
    public static final String SHIFT_EDIT = "hasAuthority('SHIFT_EDIT')";
    public static final String SUBSCRIPTION_SELL = "hasAuthority('SUBSCRIPTION_SELL')";
    public static final String PAYMENT_RECORD = "hasAuthority('PAYMENT_RECORD')";
    public static final String OFFER_MANAGE = "hasAuthority('OFFER_MANAGE')";
    public static final String PLAN_MANAGE = "hasAuthority('PLAN_MANAGE')";
    public static final String BONUS_GRANT = "hasAuthority('BONUS_GRANT')";
    public static final String PAYROLL_MANAGE = "hasAuthority('PAYROLL_MANAGE')";
    public static final String KPI_MANAGE = "hasAuthority('KPI_MANAGE')";
    public static final String REPORTS_VIEW = "hasAuthority('REPORTS_VIEW')";

    /** anyone who works with members at the front desk or sells subscriptions */
    public static final String MEMBER_READ =
            "hasAnyAuthority('MEMBER_MANAGE','SUBSCRIPTION_SELL','ATTENDANCE_RECORD')";
    public static final String SHIFT_VIEW = "hasAnyAuthority('SHIFT_EDIT','REPORTS_VIEW')";
    public static final String PAYROLL_VIEW = "hasAnyAuthority('PAYROLL_MANAGE','REPORTS_VIEW')";
    public static final String KPI_VIEW = "hasAnyAuthority('KPI_MANAGE','REPORTS_VIEW')";
    public static final String SUBSCRIPTION_VIEW =
            "hasAnyAuthority('SUBSCRIPTION_SELL','PAYMENT_RECORD','MEMBER_MANAGE','REPORTS_VIEW')";
}
