<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
         isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Something went wrong | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body class="page">

<c:set var="status"
       value="${requestScope['jakarta.servlet.error.status_code']}" />
<c:set var="detail"
       value="${requestScope['jakarta.servlet.error.message']}" />

<h1>
    <c:choose>
        <c:when test="${status == 404}">Page not found</c:when>
        <c:when test="${status == 403}">Not allowed</c:when>
        <c:when test="${status == 405}">Method not allowed</c:when>
        <c:when test="${status == 409}">Conflict</c:when>
        <c:when test="${status == 400}">Bad request</c:when>
        <c:otherwise>Something went wrong</c:otherwise>
    </c:choose>
</h1>

<c:if test="${not empty status}">
    <p class="muted">Error <c:out value="${status}" /></p>
</c:if>

<%-- Only short, deliberate messages from our own servlets are shown. --%>
<c:if test="${status != 500 and not empty detail}">
    <div class="alert alert-error" role="alert">
        <c:out value="${detail}" />
    </div>
</c:if>

<c:if test="${status == 500}">
    <p>The server could not complete the request. Please try again.</p>
</c:if>

<p>
    <a class="btn" href="${pageContext.request.contextPath}/home">Back to home</a>
</p>

</body>
</html>
