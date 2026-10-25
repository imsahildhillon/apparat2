<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Book an Instrument | Apparat</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 700px;
            margin: 40px auto;
            padding: 0 20px;
        }

        label {
            display: block;
            margin-top: 18px;
            font-weight: bold;
        }

        input, select, button {
            box-sizing: border-box;
            width: 100%;
            padding: 10px;
            margin-top: 6px;
        }

        button {
            margin-top: 24px;
            cursor: pointer;
        }

        .message {
            padding: 12px;
            background: #fff0f0;
            border: 1px solid #e0aaaa;
            margin-bottom: 18px;
        }
    </style>
</head>
<body>
<h1>Book an Instrument</h1>

<c:if test="${not empty message}">
    <div class="message" role="alert">
        <c:out value="${message}" />
    </div>
</c:if>

<form method="post"
      action="${pageContext.request.contextPath}/book">

    <label for="resourceId">Instrument</label>
    <select id="resourceId" name="resourceId" required>
        <option value="">-- Select an instrument --</option>

        <c:forEach var="resource" items="${resources}">
            <c:if test="${resource.status == 'ACTIVE'}">
                <option value="${resource.id}"
                        <c:if test="${selectedResourceId != null
                                and selectedResourceId == resource.id}">
                            selected
                        </c:if>>
                    <c:out value="${resource.name}" />
                    — <c:out value="${resource.location}" />
                </option>
            </c:if>
        </c:forEach>
    </select>

    <label for="start">Start time</label>
    <input type="datetime-local"
           id="start"
           name="start"
           value="<c:out value='${start}' />"
           required>

    <label for="end">End time</label>
    <input type="datetime-local"
           id="end"
           name="end"
           value="<c:out value='${end}' />"
           required>

    <button type="submit">Book instrument</button>
</form>

<p><a href="${pageContext.request.contextPath}/resources">
    Back to instruments
</a></p>
</body>
</html>
