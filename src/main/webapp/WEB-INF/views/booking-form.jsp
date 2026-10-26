<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
    <title>Book an Instrument | Apparat</title>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />
<div class="page">
<h1>Book an Instrument</h1>

<p class="muted">
    The lab is open from <c:out value="${openTime}" /> to
    <c:out value="${closeTime}" />. Bookings are in 30-minute steps,
    on one day.
</p>

<p class="summary"><c:out value="${policyText}" /></p>

<c:if test="${not empty message}">
    <div class="alert alert-error" role="alert">
        <c:out value="${message}" />
    </div>
</c:if>

<%-- novalidate: the page shows its own, clearer messages instead of the
     browser's pop-ups. The server checks everything again anyway. --%>
<form id="booking-form" method="post" novalidate
      action="${pageContext.request.contextPath}/book"
      data-today="${today}"
      data-max-minutes="${maxMinutes}"
      data-policy-text="<c:out value='${policyText}' />">

    <div class="alert alert-error js-error" role="alert" hidden></div>

    <div class="form-row">
        <label for="resourceId">Instrument</label>
        <select id="resourceId" name="resourceId">
            <option value="">-- Select an instrument --</option>

            <c:forEach var="resource" items="${resources}">
                <c:if test="${resource.status == 'ACTIVE'}">
                    <option value="${resource.id}"
                            <c:if test="${selectedResourceId != null
                                    and selectedResourceId == resource.id}">
                                selected
                            </c:if>>
                        <c:out value="${resource.name}" />
                        (<c:out value="${resource.location}" />)
                    </option>
                </c:if>
            </c:forEach>
        </select>
    </div>

    <div class="form-row">
        <label for="date">Date</label>
        <input type="date" id="date" name="date"
               min="<c:out value='${today}' />"
               value="<c:out value='${date}' />">
    </div>

    <div class="form-grid">
        <div class="form-row">
            <label for="startTime">Start time</label>
            <select id="startTime" name="startTime">
                <option value="">-- Start --</option>
                <c:forEach var="time" items="${startTimes}">
                    <option value="${time}"
                            <c:if test="${startTime eq time}">selected</c:if>>
                        <c:out value="${time}" />
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="form-row">
            <label for="endTime">End time</label>
            <select id="endTime" name="endTime">
                <option value="">-- End --</option>
                <c:forEach var="time" items="${endTimes}">
                    <option value="${time}"
                            <c:if test="${endTime eq time}">selected</c:if>>
                        <c:out value="${time}" />
                    </option>
                </c:forEach>
            </select>
        </div>
    </div>

    <p id="booking-summary" class="summary" aria-live="polite">
        Choose an instrument, a date and a time.
    </p>

    <button class="btn" type="submit">Book instrument</button>

    <a id="availability-link" class="btn btn-light"
       data-base="${pageContext.request.contextPath}/availability"
       href="${pageContext.request.contextPath}/availability">
        Check availability
    </a>
</form>

<p><a href="${pageContext.request.contextPath}/resources">
    Back to instruments
</a></p>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
