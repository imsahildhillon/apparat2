<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Maintenance windows | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">
    <h1>Maintenance windows</h1>
    <p class="muted">
        An instrument cannot be booked during a window. Windows follow the
        same rules as bookings (one day, on the half hour, 09:00 to 18:00).
        For a longer outage, deactivate the instrument instead.
    </p>

    <c:if test="${not empty maintenanceMessage}">
        <div class="alert alert-success" role="status">
            <c:out value="${maintenanceMessage}" />
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <div class="card">
        <h2>Add a window</h2>
        <form id="maintenance-form" method="post"
              action="${pageContext.request.contextPath}/manage/maintenance">
            <input type="hidden" name="action" value="add">
            <div class="alert alert-error js-error" role="alert" hidden></div>

            <div class="form-row">
                <label for="resourceId">Instrument</label>
                <select id="resourceId" name="resourceId" required>
                    <option value="">-- Select an instrument --</option>
                    <c:forEach var="resource" items="${resources}">
                        <option value="${resource.id}"
                                <c:if test="${resourceId eq resource.id.toString()}">selected</c:if>>
                            <c:out value="${resource.name}" />
                            (<c:out value="${resource.location}" />)
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="form-row">
                <label for="start">Start</label>
                <input type="datetime-local" id="start" name="start"
                       value="<c:out value='${start}' />" required>
            </div>

            <div class="form-row">
                <label for="end">End</label>
                <input type="datetime-local" id="end" name="end"
                       value="<c:out value='${end}' />" required>
            </div>

            <div class="form-row">
                <label for="reason">Reason</label>
                <input type="text" id="reason" name="reason" maxlength="200"
                       value="<c:out value='${reason}' />" required>
            </div>

            <button class="btn" type="submit">Add window</button>
        </form>
    </div>

    <h2>Upcoming windows</h2>

    <div class="table-wrap">

    <table>
        <thead>
        <tr>
            <th>Instrument</th>
            <th>Start</th>
            <th>End</th>
            <th>Reason</th>
            <th>Action</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="window" items="${windows}">
            <tr>
                <td><c:out value="${window.resourceName}" /></td>
                <td><c:out value="${window.startTime}" /></td>
                <td><c:out value="${window.endTime}" /></td>
                <td><c:out value="${window.reason}" /></td>
                <td>
                    <form method="post" class="inline-form js-confirm"
                          data-confirm="Remove this maintenance window?"
                          action="${pageContext.request.contextPath}/manage/maintenance">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id"
                               value="<c:out value='${window.id}' />">
                        <button class="btn btn-small btn-danger" type="submit">
                            Remove
                        </button>
                    </form>
                </td>
            </tr>
        </c:forEach>

        <c:if test="${empty windows}">
            <tr><td colspan="5">No upcoming maintenance windows.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
