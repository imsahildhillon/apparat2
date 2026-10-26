<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Manage instruments | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">
    <h1>Manage instruments</h1>

    <c:if test="${not empty manageMessage}">
        <div class="alert alert-success" role="status">
            <c:out value="${manageMessage}" />
        </div>
    </c:if>

    <c:if test="${not empty importErrors}">
        <div class="alert alert-error" role="alert">
            <strong>Rows that were not imported:</strong>
            <ul>
                <c:forEach var="line" items="${importErrors}">
                    <li><c:out value="${line}" /></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <div class="card">
        <h2>Add an instrument</h2>
        <form method="post" class="js-resource-form"
              action="${pageContext.request.contextPath}/manage/resources">
            <input type="hidden" name="action" value="add">
            <div class="alert alert-error js-error" role="alert" hidden></div>

            <div class="form-row">
                <label for="new-name">Name</label>
                <input type="text" id="new-name" name="name"
                       maxlength="100" required>
            </div>
            <div class="form-row">
                <label for="new-location">Location</label>
                <input type="text" id="new-location" name="location"
                       maxlength="100" required>
            </div>
            <button class="btn" type="submit">Add instrument</button>
        </form>
    </div>

    <div class="card">
        <h2>Import from CSV</h2>
        <p class="muted">
            One instrument per line: <code>name,location,status</code>.
            Status is optional (ACTIVE or INACTIVE, default ACTIVE).
            A header line is allowed. Maximum 500 rows and 1 MB.
        </p>
        <form id="import-form" method="post"
              enctype="multipart/form-data"
              action="${pageContext.request.contextPath}/manage/import">
            <div class="alert alert-error js-error" role="alert" hidden></div>
            <div class="form-row">
                <input type="file" name="file" accept=".csv,text/csv" required>
            </div>
            <button class="btn" type="submit">Import</button>
        </form>
    </div>

    <h2>All instruments</h2>

    <div class="table-wrap">

    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>Status</th>
            <th>Name and location</th>
            <th>Status change</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="resource" items="${resources}">
            <tr>
                <td><c:out value="${resource.id}" /></td>
                <td><c:out value="${resource.status}" /></td>
                <td>
                    <form method="post" class="js-resource-form"
                          action="${pageContext.request.contextPath}/manage/resources">
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="id"
                               value="<c:out value='${resource.id}' />">
                        <div class="alert alert-error js-error" role="alert" hidden></div>
                        <input type="text" name="name" maxlength="100"
                               value="<c:out value='${resource.name}' />" required>
                        <input type="text" name="location" maxlength="100"
                               value="<c:out value='${resource.location}' />" required>
                        <button class="btn btn-small" type="submit">Save</button>
                    </form>
                </td>
                <td>
                    <form method="post" class="inline-form js-confirm"
                          data-confirm="Change the status of this instrument?"
                          action="${pageContext.request.contextPath}/manage/resources">
                        <input type="hidden" name="action" value="status">
                        <input type="hidden" name="id"
                               value="<c:out value='${resource.id}' />">
                        <c:choose>
                            <c:when test="${resource.status == 'ACTIVE'}">
                                <input type="hidden" name="status" value="INACTIVE">
                                <button class="btn btn-small btn-danger" type="submit">
                                    Deactivate
                                </button>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="status" value="ACTIVE">
                                <button class="btn btn-small" type="submit">
                                    Activate
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </form>
                </td>
            </tr>
        </c:forEach>

        <c:if test="${empty resources}">
            <tr><td colspan="4">No instruments yet.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
