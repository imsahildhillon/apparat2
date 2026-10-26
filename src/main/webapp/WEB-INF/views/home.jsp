<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Home | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">

    <div class="hero">
        <h1>
            <c:out value="${greeting}" />,
            <c:out value="${sessionScope.sessionUser.name}" />
        </h1>
        <p>
            <span class="badge">
                <c:out value="${sessionScope.sessionUser.role.label}" />
            </span>
            <span class="muted"><c:out value="${policyText}" /></span>
        </p>
    </div>

    <div class="tiles">
        <div class="tile">
            <div class="tile-label">Active bookings</div>
            <div class="tile-value"><c:out value="${activeBookings}" /></div>
            <div class="tile-sub"><c:out value="${bookingLimit}" /></div>
        </div>

        <div class="tile">
            <div class="tile-label">Next booking</div>
            <c:choose>
                <c:when test="${not empty nextBooking}">
                    <div class="tile-text"><c:out value="${nextBooking.instrument}" /></div>
                    <div class="tile-sub"><c:out value="${nextBooking.when}" /></div>
                </c:when>
                <c:otherwise>
                    <div class="tile-text">None yet</div>
                    <div class="tile-sub">Book an instrument to get started</div>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="tile">
            <div class="tile-label">Instruments available</div>
            <div class="tile-value"><c:out value="${instrumentsActive}" /></div>
            <div class="tile-sub">of <c:out value="${instrumentsTotal}" /></div>
        </div>
    </div>

    <div class="actions">
        <a class="btn" href="${pageContext.request.contextPath}/book">Book an instrument</a>
        <a class="btn btn-light" href="${pageContext.request.contextPath}/availability">Check availability</a>
        <a class="btn btn-light" href="${pageContext.request.contextPath}/bookings">My bookings</a>
    </div>

    <h2>Your next bookings</h2>

    <c:choose>
        <c:when test="${not empty upcomingBookings}">
            <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Instrument</th>
                    <th>When</th>
                    <th>Booking</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="booking" items="${upcomingBookings}">
                    <tr>
                        <td><c:out value="${booking.instrument}" /></td>
                        <td><c:out value="${booking.when}" /></td>
                        <td>#<c:out value="${booking.id}" /></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
</div>
            <p class="muted">
                <a href="${pageContext.request.contextPath}/bookings">See all and cancel</a> ·
                <a href="${pageContext.request.contextPath}/export/bookings">Download as CSV</a>
            </p>
        </c:when>
        <c:otherwise>
            <div class="alert alert-info">
                You have no upcoming bookings.
                <a href="${pageContext.request.contextPath}/book">Book an instrument</a>
                or <a href="${pageContext.request.contextPath}/availability">check what is free</a>.
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${sessionScope.permissions.MANAGE_MAINTENANCE}">
        <div class="card">
            <h2>Maintenance coming up</h2>
            <c:choose>
                <c:when test="${not empty windows}">
                    <ul class="plain-list">
                        <c:forEach var="window" items="${windows}">
                            <li>
                                <strong><c:out value="${window.instrument}" /></strong>,
                                <c:out value="${window.when}" />
                                <span class="muted">(<c:out value="${window.reason}" />)</span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:when>
                <c:otherwise>
                    <p class="muted">No maintenance is scheduled.</p>
                </c:otherwise>
            </c:choose>
            <p>
                <a href="${pageContext.request.contextPath}/manage/maintenance">Maintenance windows</a>
                <c:if test="${sessionScope.permissions.MANAGE_RESOURCES}">
                    · <a href="${pageContext.request.contextPath}/manage/resources">Manage instruments</a>
                </c:if>
                <c:if test="${sessionScope.permissions.VIEW_REPORTS}">
                    · <a href="${pageContext.request.contextPath}/reports">Utilization report</a>
                </c:if>
            </p>
        </div>
    </c:if>

    <c:if test="${sessionScope.permissions.MANAGE_USERS}">
        <div class="card">
            <h2>People</h2>
            <div class="tiles tiles-small">
                <c:forEach var="entry" items="${usersByRole}">
                    <div class="tile">
                        <div class="tile-label"><c:out value="${entry.key}" /></div>
                        <div class="tile-value"><c:out value="${entry.value}" /></div>
                    </div>
                </c:forEach>
            </div>
            <p class="muted">
                Active accounts by role.
                <c:if test="${inactiveUsers > 0}">
                    <c:out value="${inactiveUsers}" /> deactivated.
                </c:if>
            </p>
            <p>
                <a href="${pageContext.request.contextPath}/admin/users">Manage users</a>
            </p>
        </div>
    </c:if>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
