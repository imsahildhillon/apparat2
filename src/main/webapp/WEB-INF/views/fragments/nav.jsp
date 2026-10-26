<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- Links appear only for roles that hold the permission (see Role.java). --%>
<nav class="nav">
    <span class="brand">Apparat</span>

    <a href="${pageContext.request.contextPath}/home">Home</a>
    <a href="${pageContext.request.contextPath}/resources">Instruments</a>
    <a href="${pageContext.request.contextPath}/availability">Availability</a>
    <a href="${pageContext.request.contextPath}/book">Book</a>
    <a href="${pageContext.request.contextPath}/bookings">My bookings</a>

    <c:if test="${sessionScope.permissions.MANAGE_RESOURCES}">
        <a href="${pageContext.request.contextPath}/manage/resources">Manage</a>
    </c:if>
    <c:if test="${sessionScope.permissions.MANAGE_MAINTENANCE}">
        <a href="${pageContext.request.contextPath}/manage/maintenance">Maintenance</a>
    </c:if>
    <c:if test="${sessionScope.permissions.VIEW_REPORTS}">
        <a href="${pageContext.request.contextPath}/reports">Reports</a>
    </c:if>
    <c:if test="${sessionScope.permissions.MANAGE_USERS}">
        <a href="${pageContext.request.contextPath}/admin/users">Users</a>
    </c:if>

    <span class="spacer"></span>

    <span class="who">
        <c:out value="${sessionScope.sessionUser.name}" />
        (<c:out value="${sessionScope.sessionUser.role.label}" />)
    </span>

    <form method="post"
          action="${pageContext.request.contextPath}/logout">
        <button class="link-button" type="submit">Log out</button>
    </form>
</nav>
