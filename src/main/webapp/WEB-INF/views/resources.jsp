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
    <title>Available Instruments | Apparat</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 900px;
            margin: 40px auto;
            padding: 0 20px;
        }

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
    </style>
</head>
<body>
<h1>Available Instruments</h1>

<table>
    <thead>
    <tr>
        <th>Name</th>
        <th>Location</th>
        <th>Status</th>
    </tr>
    </thead>

    <tbody>
    <c:forEach var="resource" items="${resources}">
        <tr>
            <td><c:out value="${resource.name}" /></td>
            <td><c:out value="${resource.location}" /></td>
            <td><c:out value="${resource.status}" /></td>
        </tr>
    </c:forEach>

    <c:if test="${empty resources}">
        <tr>
            <td colspan="3">No instruments found.</td>
        </tr>
    </c:if>
    </tbody>
</table>
</body>
</html>
