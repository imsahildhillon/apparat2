<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Utilization report | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">
    <h1>Utilization report</h1>
    <p class="muted">
        Booked hours as a share of the hours the lab is open
        (09:00 to 18:00 every day in the range).
    </p>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <form id="report-form" method="get" class="card"
          action="${pageContext.request.contextPath}/reports">
        <div class="alert alert-error js-error" role="alert" hidden></div>

        <div class="form-row">
            <label for="from">From</label>
            <input type="date" id="from" name="from"
                   value="<c:out value='${from}' />" required>
        </div>
        <div class="form-row">
            <label for="to">To</label>
            <input type="date" id="to" name="to"
                   value="<c:out value='${to}' />" required>
        </div>

        <button class="btn" type="submit">Show report</button>

        <c:if test="${not empty rows}">
            <a class="btn"
               href="${pageContext.request.contextPath}/reports?from=${from}&to=${to}&format=csv">
                Download CSV
            </a>
        </c:if>
    </form>

    <c:if test="${not empty rows}">
        <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th>Instrument</th>
                <th>Bookings</th>
                <th>Booked hours</th>
                <th>Utilization</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${rows}">
                <tr>
                    <td><c:out value="${row.resourceName}" /></td>
                    <td><c:out value="${row.bookings}" /></td>
                    <td><c:out value="${row.bookedHours}" /></td>
                    <td><c:out value="${row.utilizationPercent}" />%</td>
                </tr>
            </c:forEach>
            <tr>
                <th>Total</th>
                <th><c:out value="${totalBookings}" /></th>
                <th><c:out value="${totalHours}" /></th>
                <th></th>
            </tr>
            </tbody>
        </table>
</div>
    </c:if>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
