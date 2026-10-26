<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Users | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>

<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />

<div class="page">
    <h1>Users</h1>
    <p class="muted">
        Create accounts, change roles, set a new password, or deactivate an
        account. A deactivated user is logged out at once and cannot log in.
        The system always keeps at least one active administrator.
    </p>

    <c:if test="${not empty adminMessage}">
        <div class="alert alert-success" role="status">
            <c:out value="${adminMessage}" />
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <div class="card">
        <h2>Add a user</h2>
        <form id="create-user-form" method="post" novalidate
              action="${pageContext.request.contextPath}/admin/users">
            <input type="hidden" name="action" value="create">
            <div class="alert alert-error js-error" role="alert" hidden></div>

            <div class="form-grid">
                <div class="form-row">
                    <label for="new-name">Name</label>
                    <input type="text" id="new-name" name="name" maxlength="100"
                           value="<c:out value='${newName}' />">
                </div>
                <div class="form-row">
                    <label for="new-email">Email</label>
                    <input type="email" id="new-email" name="email" maxlength="255"
                           value="<c:out value='${newEmail}' />">
                </div>
                <div class="form-row">
                    <label for="new-role">Role</label>
                    <select id="new-role" name="role">
                        <c:forEach var="role" items="${roles}">
                            <option value="${role}"
                                    <c:if test="${newRole eq role.name()}">selected</c:if>>
                                <c:out value="${role.label}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-row">
                    <label for="new-password">Temporary password</label>
                    <input type="password" id="new-password" name="password"
                           autocomplete="new-password" maxlength="100">
                </div>
            </div>
            <p class="muted">
                At least 8 characters, with a letter and a digit.
            </p>
            <button class="btn" type="submit">Create user</button>
        </form>
    </div>

    <h2>All users</h2>

    <div class="table-wrap">

    <table>
        <thead>
        <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Password</th>
            <th>Account</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="user" items="${users}">
            <tr>
                <td>
                    <c:out value="${user.name}" />
                    <c:if test="${user.id == myId}"><span class="muted">(you)</span></c:if>
                </td>
                <td><c:out value="${user.email}" /></td>
                <td>
                    <c:choose>
                        <c:when test="${user.id == myId}">
                            <c:out value="${user.role.label}" />
                        </c:when>
                        <c:otherwise>
                            <form method="post" class="inline-form"
                                  action="${pageContext.request.contextPath}/admin/users">
                                <input type="hidden" name="action" value="role">
                                <input type="hidden" name="id"
                                       value="<c:out value='${user.id}' />">
                                <select name="role" aria-label="Role of <c:out value='${user.name}' />">
                                    <c:forEach var="role" items="${roles}">
                                        <option value="${role}"
                                                <c:if test="${user.role == role}">selected</c:if>>
                                            <c:out value="${role.label}" />
                                        </option>
                                    </c:forEach>
                                </select>
                                <button class="btn btn-small" type="submit">Save</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${user.active}">Active</c:when>
                        <c:otherwise><strong>Deactivated</strong></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <form method="post" class="inline-form js-password-form" novalidate
                          action="${pageContext.request.contextPath}/admin/users">
                        <input type="hidden" name="action" value="password">
                        <input type="hidden" name="id"
                               value="<c:out value='${user.id}' />">
                        <input type="password" name="password" placeholder="New password"
                               autocomplete="new-password" maxlength="100"
                               aria-label="New password for <c:out value='${user.name}' />">
                        <button class="btn btn-small" type="submit">Set</button>
                    </form>
                </td>
                <td>
                    <c:if test="${user.id != myId}">
                        <form method="post" class="inline-form js-confirm"
                              data-confirm="${user.active ? 'Deactivate this account? The user is logged out at once.' : 'Activate this account?'}"
                              action="${pageContext.request.contextPath}/admin/users">
                            <input type="hidden" name="action" value="active">
                            <input type="hidden" name="id"
                                   value="<c:out value='${user.id}' />">
                            <input type="hidden" name="active"
                                   value="${user.active ? 'false' : 'true'}">
                            <button class="btn btn-small ${user.active ? 'btn-danger' : ''}"
                                    type="submit">
                                ${user.active ? 'Deactivate' : 'Activate'}
                            </button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
