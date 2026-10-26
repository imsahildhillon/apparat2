/*
 * Small client-side helpers for quick feedback. The server checks every
 * value again, because a browser can never be trusted.
 */
document.addEventListener("DOMContentLoaded", function () {

    // Show a message next to the first field that fails a check.
    function fail(form, message) {
        var box = form.querySelector(".js-error");
        if (box) {
            box.textContent = message;
            box.hidden = false;
        } else {
            alert(message);
        }
        return false;
    }

    function clear(form) {
        var box = form.querySelector(".js-error");
        if (box) {
            box.hidden = true;
        }
    }

    // Login form: email shape and a non-empty password.
    var login = document.getElementById("login-form");
    if (login) {
        login.addEventListener("submit", function (event) {
            clear(login);
            var email = login.elements["email"].value.trim();
            var password = login.elements["password"].value;
            if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
                event.preventDefault();
                fail(login, "Please enter a valid email address.");
            } else if (password.length === 0) {
                event.preventDefault();
                fail(login, "Please enter your password.");
            }
        });
    }

    // Add / edit instrument forms: name and location are required.
    document.querySelectorAll("form.js-resource-form").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            clear(form);
            var name = form.elements["name"].value.trim();
            var location = form.elements["location"].value.trim();
            if (name === "" || location === "") {
                event.preventDefault();
                fail(form, "Name and location are required.");
            } else if (name.length > 100 || location.length > 100) {
                event.preventDefault();
                fail(form, "Name and location must be at most 100 characters.");
            }
        });
    });

    // CSV import: only .csv files, up to 1 MB.
    var importForm = document.getElementById("import-form");
    if (importForm) {
        importForm.addEventListener("submit", function (event) {
            clear(importForm);
            var input = importForm.elements["file"];
            if (!input.files || input.files.length === 0) {
                event.preventDefault();
                fail(importForm, "Choose a CSV file first.");
            } else if (!input.files[0].name.toLowerCase().endsWith(".csv")) {
                event.preventDefault();
                fail(importForm, "Only .csv files are accepted.");
            } else if (input.files[0].size > 1024 * 1024) {
                event.preventDefault();
                fail(importForm, "The file is larger than 1 MB.");
            }
        });
    }

    // Report range: the start date cannot be after the end date.
    var report = document.getElementById("report-form");
    if (report) {
        report.addEventListener("submit", function (event) {
            clear(report);
            var from = report.elements["from"].value;
            var to = report.elements["to"].value;
            if (from && to && from > to) {
                event.preventDefault();
                fail(report, "The start date must not be after the end date.");
            }
        });
    }

    // Maintenance form: end after start, and a reason is required.
    var maintenance = document.getElementById("maintenance-form");
    if (maintenance) {
        maintenance.addEventListener("submit", function (event) {
            clear(maintenance);
            var start = maintenance.elements["start"].value;
            var end = maintenance.elements["end"].value;
            var reason = maintenance.elements["reason"].value.trim();
            if (!start || !end) {
                event.preventDefault();
                fail(maintenance, "Please choose a start and an end time.");
            } else if (start >= end) {
                event.preventDefault();
                fail(maintenance, "The end time must be after the start time.");
            } else if (reason === "") {
                event.preventDefault();
                fail(maintenance, "Please give a reason.");
            }
        });
    }

    // Booking form: only valid choices, live summary, clear messages.
    var booking = document.getElementById("booking-form");
    if (booking) {
        var resourceSel = booking.elements["resourceId"];
        var dateInput = booking.elements["date"];
        var startSel = booking.elements["startTime"];
        var endSel = booking.elements["endTime"];
        var summary = document.getElementById("booking-summary");
        var availabilityLink = document.getElementById("availability-link");
        var today = booking.getAttribute("data-today");

        var toMinutes = function (value) {
            var parts = value.split(":");
            return parseInt(parts[0], 10) * 60 + parseInt(parts[1], 10);
        };
        var pad = function (n) { return (n < 10 ? "0" : "") + n; };
        var toText = function (minutes) {
            return pad(Math.floor(minutes / 60)) + ":" + pad(minutes % 60);
        };
        var minutesNow = function () {
            var now = new Date();
            return now.getHours() * 60 + now.getMinutes();
        };

        var refreshBooking = function () {
            // Today: times that have already passed cannot be chosen.
            var passed = dateInput.value === today ? minutesNow() : -1;
            Array.prototype.forEach.call(startSel.options, function (o) {
                if (o.value !== "") {
                    o.disabled = toMinutes(o.value) <= passed;
                }
            });
            if (startSel.value !== "" && startSel.selectedOptions[0].disabled) {
                startSel.value = "";
            }

            // The end must be later than the start.
            var start = startSel.value !== "" ? toMinutes(startSel.value) : -1;
            // ...and not longer than this user's role allows.
            var maxMinutes = parseInt(booking.getAttribute("data-max-minutes"), 10) || 540;
            Array.prototype.forEach.call(endSel.options, function (o) {
                if (o.value !== "") {
                    o.disabled = toMinutes(o.value) <= start
                            || (start >= 0 && toMinutes(o.value) - start > maxMinutes);
                }
            });
            if (endSel.value !== "" && endSel.selectedOptions[0].disabled) {
                endSel.value = "";
            }

            // Link to the availability page for the same instrument and day.
            var link = availabilityLink.getAttribute("data-base");
            var query = [];
            if (resourceSel.value !== "") {
                query.push("resourceId=" + encodeURIComponent(resourceSel.value));
            }
            if (dateInput.value !== "") {
                query.push("date=" + encodeURIComponent(dateInput.value));
            }
            availabilityLink.href = query.length ? link + "?" + query.join("&") : link;

            // Plain-words summary of the choice.
            if (resourceSel.value !== "" && dateInput.value !== ""
                    && startSel.value !== "" && endSel.value !== "") {
                var minutes = toMinutes(endSel.value) - toMinutes(startSel.value);
                var hours = Math.floor(minutes / 60);
                var rest = minutes % 60;
                var length = (hours > 0 ? hours + " h " : "")
                        + (rest > 0 ? rest + " min" : "");
                var day = new Date(dateInput.value + "T00:00:00");
                var dayText = isNaN(day.getTime()) ? dateInput.value
                        : day.toLocaleDateString("en-GB", {
                            weekday: "long", day: "numeric",
                            month: "long", year: "numeric"
                        });
                summary.textContent = resourceSel.selectedOptions[0].text.trim()
                        + ", " + dayText + ", " + startSel.value + " - "
                        + endSel.value + " (" + length.trim() + ")";
            } else if (dateInput.value === today
                    && !Array.prototype.some.call(startSel.options, function (o) {
                        return o.value !== "" && !o.disabled;
                    })) {
                summary.textContent = "No times are left today. "
                        + "Please choose another date.";
            } else {
                summary.textContent = "Choose an instrument, a date and a time.";
            }
        };

        startSel.addEventListener("change", function () {
            // Offer a 30-minute booking straight away.
            if (startSel.value !== "" && endSel.value === "") {
                var suggested = toText(toMinutes(startSel.value) + 30);
                var option = Array.prototype.find.call(endSel.options,
                        function (o) { return o.value === suggested; });
                if (option) {
                    endSel.value = suggested;
                }
            }
            refreshBooking();
        });
        [resourceSel, dateInput, endSel].forEach(function (field) {
            field.addEventListener("change", refreshBooking);
        });
        refreshBooking();

        booking.addEventListener("submit", function (event) {
            clear(booking);
            var problem = null;

            if (resourceSel.value === "") {
                problem = "Please choose an instrument.";
            } else if (dateInput.value === "") {
                problem = "Please choose a date.";
            } else if (startSel.value === "") {
                problem = "Please choose a start time.";
            } else if (endSel.value === "") {
                problem = "Please choose an end time.";
            } else if (toMinutes(endSel.value) <= toMinutes(startSel.value)) {
                problem = "The end time must be after the start time.";
            } else if (toMinutes(endSel.value) - toMinutes(startSel.value)
                    > (parseInt(booking.getAttribute("data-max-minutes"), 10) || 540)) {
                problem = "That booking is longer than your role allows. "
                        + booking.getAttribute("data-policy-text");
            } else if (dateInput.value < today
                    || (dateInput.value === today
                        && toMinutes(startSel.value) <= minutesNow())) {
                problem = "That time has already passed. "
                        + "Please choose a later start time.";
            }

            if (problem !== null) {
                event.preventDefault();
                fail(booking, problem);
            }
        });
    }

    // Passwords: the same rules the server enforces.
    var passwordProblem = function (value) {
        if (value.length < 8) {
            return "The password must be at least 8 characters long.";
        }
        if (!/[A-Za-z]/.test(value) || !/[0-9]/.test(value)) {
            return "The password must contain at least one letter and one digit.";
        }
        return null;
    };

    // Add-user form.
    var createUser = document.getElementById("create-user-form");
    if (createUser) {
        createUser.addEventListener("submit", function (event) {
            clear(createUser);
            var problem = null;
            if (createUser.elements["name"].value.trim() === "") {
                problem = "Please enter a name.";
            } else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(
                    createUser.elements["email"].value.trim())) {
                problem = "Please enter a valid email address.";
            } else {
                problem = passwordProblem(createUser.elements["password"].value);
            }
            if (problem !== null) {
                event.preventDefault();
                fail(createUser, problem);
            }
        });
    }

    // Set-password forms in the user table.
    document.querySelectorAll("form.js-password-form").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            var problem = passwordProblem(form.elements["password"].value);
            if (problem !== null) {
                event.preventDefault();
                alert(problem);
            }
        });
    });

    // Ask before changing an instrument's status.
    document.querySelectorAll("form.js-confirm").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!confirm(form.getAttribute("data-confirm") || "Are you sure?")) {
                event.preventDefault();
            }
        });
    });
});
