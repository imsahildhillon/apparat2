<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Availability | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">
    <h1>Instrument availability</h1>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <form method="get"
          action="${pageContext.request.contextPath}/availability"
          class="card">

        <div class="form-row">
            <label for="resourceId">Instrument</label>
            <select id="resourceId" name="resourceId" required>
                <option value="">-- Select an instrument --</option>
                <c:forEach var="resource" items="${resources}">
                    <option value="${resource.id}"
                            <c:if test="${selected.id == resource.id}">selected</c:if>>
                        <c:out value="${resource.name}" />
                        (<c:out value="${resource.location}" />)
                    </option>
                </c:forEach>
            </select>
        </div>

        <div class="form-row">
            <label for="date">Date</label>
            <input type="date" id="date" name="date"
                   value="<c:out value='${date}' />" required>
        </div>

        <button class="btn" type="submit">Show availability</button>
    </form>

    <c:if test="${not empty selected}">
        <h2>
            <c:out value="${selected.name}" /> on
            <c:out value="${date}" />
        </h2>

        <c:if test="${selected.status != 'ACTIVE'}">
            <div class="alert alert-info">
                This instrument is currently not available for booking.
            </div>
        </c:if>

        <div class="table-wrap">

        <table>
            <thead>
            <tr>
                <th>Time slot</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="slot" items="${grid}">
                <tr class="${slot.maintenance ? 'slot-maint' : (slot.booked ? 'slot-booked' : 'slot-free')}">
                    <td><c:out value="${slot.label}" /></td>
                    <td>${slot.maintenance ? 'Maintenance' : (slot.booked ? 'Booked' : 'Free')}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
</div>

        <p>
            <a class="btn"
               href="${pageContext.request.contextPath}/book?resourceId=${selected.id}&date=${date}">
                Book this instrument
            </a>
        </p>
    </c:if>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
