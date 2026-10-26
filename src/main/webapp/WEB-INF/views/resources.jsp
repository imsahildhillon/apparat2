<%--
  Created by IntelliJ IDEA.
  User: dhillon
  Date: 25/10/26
  Time: 10:47 pm
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
    <title>Available Instruments | Apparat</title>
    <style>

        table {
            width: 100%;
            border-collapse: collapse;
        }

        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }

        th {
            background: #f2f2f2;
        }

        .success {
            padding: 12px;
            margin-bottom: 20px;
            background: #eaf7ea;
            border: 1px solid #a5d6a7;
            color: #216e39;
        }

        .book-link {
            color: #075ec8;
            text-decoration: none;
            font-weight: bold;
        }

        .book-link:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/fragments/nav.jsp" />
<div class="page">

<h1>Available Instruments</h1>

<c:if test="${not empty bookedId}">
    <p class="success" role="status">
        Booking #<c:out value="${bookedId}" /> confirmed!
    </p>
</c:if>

<p>
    <a href="${pageContext.request.contextPath}/book">
        Book an instrument
    </a>
</p>

<div class="table-wrap">

<table>
    <thead>
    <tr>
        <th>Name</th>
        <th>Location</th>
        <th>Status</th>
        <th>Action</th>
    </tr>
    </thead>

    <tbody>
    <c:forEach var="resource" items="${resources}">
        <tr>
            <td><c:out value="${resource.name}" /></td>
            <td><c:out value="${resource.location}" /></td>
            <td><c:out value="${resource.status}" /></td>
            <td>
                <c:if test="${resource.status == 'ACTIVE'}">
                    <a class="book-link"
                       href="${pageContext.request.contextPath}/book?resourceId=${resource.id}">
                        Book
                    </a>
                </c:if>
                <c:if test="${resource.status != 'ACTIVE'}">
                    Not available
                </c:if>
            </td>
        </tr>
    </c:forEach>

    <c:if test="${empty resources}">
        <tr>
            <td colspan="4">No instruments found.</td>
        </tr>
    </c:if>
    </tbody>
</table>
</div>

</div>
</body>
</html>
