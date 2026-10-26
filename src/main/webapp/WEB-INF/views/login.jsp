<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Log in | Apparat</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/app.css">
</head>
<body class="page">

<div class="login-box">
    <h1>Apparat</h1>
    <p class="muted">Shared laboratory instrument scheduling</p>

    <c:if test="${not empty message}">
        <div class="alert alert-info" role="status">
            <c:out value="${message}" />
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-error" role="alert">
            <c:out value="${error}" />
        </div>
    </c:if>

    <form id="login-form" method="post"
          action="${pageContext.request.contextPath}/login">

        <div class="alert alert-error js-error" role="alert" hidden></div>

        <div class="form-row">
            <label for="email">Email</label>
            <input type="email" id="email" name="email"
                   value="<c:out value='${email}' />"
                   autocomplete="username" required>
        </div>

        <div class="form-row">
            <label for="password">Password</label>
            <input type="password" id="password" name="password"
                   autocomplete="current-password" required>
        </div>

        <button class="btn" type="submit">Log in</button>
    </form>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
