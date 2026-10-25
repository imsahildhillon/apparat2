
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>My Bookings | Apparat</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      max-width: 1000px;
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

    .cancel-button {
      background: #b42318;
      color: white;
      border: 0;
      padding: 8px 12px;
      cursor: pointer;
      border-radius: 4px;
    }

    .message {
      padding: 12px;
      margin-bottom: 16px;
      background: #eaf7ea;
    }
  </style>
</head>
<body>

<h1>My Bookings</h1>

<c:if test="${not empty requestScope.bookingMessage}">
  <p class="message" role="status">
    <c:out value="${requestScope.bookingMessage}" />
  </p>
</c:if>

<p>
  <a href="${pageContext.request.contextPath}/resources">
    Browse instruments
  </a>
</p>

<table>
  <thead>
  <tr>
    <th>Booking ID</th>
    <th>Instrument</th>
    <th>Start</th>
    <th>End</th>
    <th>Status</th>
    <th>Action</th>
  </tr>
  </thead>

  <tbody>
  <c:forEach var="booking" items="${bookings}">
    <tr>
      <td><c:out value="${booking.id}" /></td>
      <td><c:out value="${booking.resourceName}" /></td>
      <td><c:out value="${booking.startTime}" /></td>
      <td><c:out value="${booking.endTime}" /></td>
      <td><c:out value="${booking.status}" /></td>

      <td>
        <c:if test="${booking.status == 'CONFIRMED'}">
          <form method="post"
                action="${pageContext.request.contextPath}/cancel">
            <input type="hidden"
                   name="bookingId"
                   value="<c:out value='${booking.id}' />" />

            <button class="cancel-button" type="submit">
              Cancel
            </button>
          </form>
        </c:if>

        <c:if test="${booking.status != 'CONFIRMED'}">
          N/A
        </c:if>
      </td>
    </tr>
  </c:forEach>

  <c:if test="${empty bookings}">
    <tr>
      <td colspan="6">You have no bookings yet.</td>
    </tr>
  </c:if>
  </tbody>
</table>

</body>
</html>
