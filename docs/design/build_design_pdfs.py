#!/usr/bin/env python3
"""
Builds two design guides (PDF): one for Nazrin's slice, one for Shourya's.
File sizes are counted from the real source files at build time.
All test and check results quoted here were measured on this project.
"""
import os
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.units import inch
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table,
    TableStyle, HRFlowable, KeepTogether, PageBreak, NextPageTemplate, CondPageBreak
)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA = "src/main/java/com/apparat/"
WEB = "src/main/webapp/"
VIEWS = WEB + "WEB-INF/views/"

NAVY = colors.HexColor("#0B3B5C")
TEAL = colors.HexColor("#1C7293")
DARK = colors.HexColor("#1B2733")
MUTED = colors.HexColor("#5B6B79")
CARD = colors.HexColor("#F3F6F8")
LINE = colors.HexColor("#D7E0E5")
GREEN = colors.HexColor("#1F7A4D")
AMBER = colors.HexColor("#B7791F")
RED = colors.HexColor("#B42318")
WHITE = colors.white
PAGE_W, PAGE_H = LETTER
M = 0.8 * inch
COL = PAGE_W - 2 * M

ST = {
    "h1": ParagraphStyle("h1", fontName="Helvetica-Bold", fontSize=15, textColor=NAVY, leading=19, spaceBefore=11, spaceAfter=5, keepWithNext=1),
    "h2": ParagraphStyle("h2", fontName="Helvetica-Bold", fontSize=11, textColor=TEAL, leading=14, spaceBefore=7, spaceAfter=3, keepWithNext=1),
    "p": ParagraphStyle("p", fontName="Helvetica", fontSize=9.4, textColor=DARK, leading=13.2, spaceAfter=4),
    "small": ParagraphStyle("small", fontName="Helvetica", fontSize=8.3, textColor=MUTED, leading=11.5, spaceAfter=3),
    "cell": ParagraphStyle("cell", fontName="Helvetica", fontSize=8.6, textColor=DARK, leading=11.2),
    "cellb": ParagraphStyle("cellb", fontName="Helvetica-Bold", fontSize=8.6, textColor=DARK, leading=11.2),
    "head": ParagraphStyle("head", fontName="Helvetica-Bold", fontSize=8.6, textColor=WHITE, leading=11),
    "bul": ParagraphStyle("bul", fontName="Helvetica", fontSize=9.3, textColor=DARK, leading=12.9, leftIndent=12, bulletIndent=2, spaceAfter=1.5),
    "q": ParagraphStyle("q", fontName="Helvetica-Bold", fontSize=9.6, textColor=NAVY, leading=12.5, spaceBefore=5, spaceAfter=1.5),
    "a": ParagraphStyle("a", fontName="Helvetica", fontSize=9.2, textColor=DARK, leading=12.8, leftIndent=10, spaceAfter=2),
    "code": ParagraphStyle("code", fontName="Courier", fontSize=8.2, textColor=DARK, leading=10.6),
    "box": ParagraphStyle("box", fontName="Helvetica-Bold", fontSize=8.2, textColor=WHITE, leading=10.2, alignment=1),
    "arrow": ParagraphStyle("arrow", fontName="Helvetica-Bold", fontSize=11, textColor=MUTED, leading=12, alignment=1),
}

def P(t, s="p"): return Paragraph(t, ST[s])
def H1(t):
    rule = HRFlowable(width="100%", thickness=0.7, color=LINE, spaceAfter=5)
    # Start a new page when too little room is left, so a heading is never stranded.
    return [CondPageBreak(2.0 * inch), P(t, "h1"), rule]
def H2(t): return P(t, "h2")
def bullets(items): return [Paragraph(i, ST["bul"], bulletText="-") for i in items]

def lines_of(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as f:
        return sum(1 for _ in f)

def table(header, rows, widths, zebra=True):
    data = [[Paragraph(h, ST["head"]) for h in header]]
    for r in rows:
        data.append([Paragraph(str(c), ST["cell"]) for c in r])
    t = Table(data, colWidths=widths, repeatRows=1)
    style = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 3.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
    ]
    if zebra:
        for i in range(1, len(data)):
            if i % 2 == 0:
                style.append(("BACKGROUND", (0, i), (-1, i), CARD))
    t.setStyle(TableStyle(style))
    return t

def status_table(rows):
    """rows: (review item, status, where). Status is Done / Partial / Not built."""
    data = [[Paragraph(h, ST["head"]) for h in ("Review 1 item", "Status", "Where it lives")]]
    cols = {"Done": GREEN, "Partial": AMBER, "Not built": RED}
    for item, status, where in rows:
        data.append([Paragraph(item, ST["cell"]),
                     Paragraph(f'<font color="{cols[status].hexval()}"><b>{status}</b></font>'.replace("0x", "#"), ST["cell"]),
                     Paragraph(where, ST["cell"])])
    t = Table(data, colWidths=[2.3 * inch, 0.8 * inch, COL - 3.1 * inch], repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 3.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
    ]))
    return t

def flow(labels, fills=None):
    """A single row of boxes joined by arrows."""
    n = len(labels)
    arrow_w = 0.2 * inch
    box_w = (COL - arrow_w * (n - 1)) / n
    cells, widths = [], []
    for i, lab in enumerate(labels):
        cells.append(Paragraph(lab, ST["box"])); widths.append(box_w)
        if i < n - 1:
            cells.append(Paragraph(">", ST["arrow"])); widths.append(arrow_w)
    t = Table([cells], colWidths=widths, rowHeights=[0.56 * inch])
    style = [("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
             ("LEFTPADDING", (0, 0), (-1, -1), 3), ("RIGHTPADDING", (0, 0), (-1, -1), 3)]
    for i in range(n):
        fill = (fills[i] if fills else NAVY)
        style.append(("BACKGROUND", (i * 2, 0), (i * 2, 0), fill))
    t.setStyle(TableStyle(style))
    return t

def code(text):
    t = Table([[Paragraph(text.replace("\n", "<br/>").replace(" ", "&nbsp;"), ST["code"])]], colWidths=[COL])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), CARD), ("BOX", (0, 0), (-1, -1), 0.4, LINE),
                           ("LEFTPADDING", (0, 0), (-1, -1), 8), ("TOPPADDING", (0, 0), (-1, -1), 5),
                           ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
    return t

def callout(title, text, color=TEAL):
    t = Table([[Paragraph(f"<b>{title}</b><br/>{text}", ST["cell"])]], colWidths=[COL])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), CARD), ("LINEBEFORE", (0, 0), (0, 0), 3, color),
                           ("LEFTPADDING", (0, 0), (-1, -1), 10), ("TOPPADDING", (0, 0), (-1, -1), 6),
                           ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    return t

def qa(pairs):
    out = []
    for i, (q, a) in enumerate(pairs, 1):
        out.append(KeepTogether([P(f"Q{i}. {q}", "q"), P(a, "a")]))
    return out

def files_table(entries):
    rows, total = [], 0
    for rel, what in entries:
        n = lines_of(rel)
        total += n
        rows.append((rel.replace("src/main/", "").replace("java/com/apparat/", ""), what, n))
    rows.append(("<b>Total</b>", "", f"<b>{total}</b>"))
    return table(("File", "Job", "Lines"), rows, [2.55 * inch, COL - 3.25 * inch, 0.7 * inch])

# ---------------------------------------------------------------- page chrome
def make_doc(path, title, footer_text, cover_fn):
    state = {"footer": footer_text}
    def chrome(canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(LINE); canvas.setLineWidth(0.6)
        canvas.line(M, 0.62 * inch, PAGE_W - M, 0.62 * inch)
        canvas.setFont("Helvetica", 8); canvas.setFillColor(MUTED)
        canvas.drawString(M, 0.45 * inch, state["footer"])
        canvas.drawRightString(PAGE_W - M, 0.45 * inch, str(doc.page))
        canvas.setFont("Helvetica-Bold", 8); canvas.setFillColor(TEAL)
        canvas.drawString(M, PAGE_H - 0.55 * inch, "APPARAT  -  DESIGN GUIDE")
        canvas.setStrokeColor(LINE); canvas.line(M, PAGE_H - 0.62 * inch, PAGE_W - M, PAGE_H - 0.62 * inch)
        canvas.restoreState()
    def cover(canvas, doc):
        cover_fn(canvas)
    doc = BaseDocTemplate(path, pagesize=LETTER, leftMargin=M, rightMargin=M, topMargin=0.9 * inch,
                          bottomMargin=0.85 * inch, title=title, author="Apparat team")
    frame_c = Frame(M, 0.85 * inch, COL, PAGE_H - 1.75 * inch, id="c", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    doc.addPageTemplates([PageTemplate(id="Cover", frames=[frame_c], onPage=cover),
                          PageTemplate(id="Body", frames=[frame_c], onPage=chrome)])
    return doc

def cover_factory(owner, roll, heading, tagline, items):
    def draw(c):
        c.saveState()
        c.setFillColor(NAVY); c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
        c.setFont("Helvetica-Bold", 11); c.setFillColor(TEAL)
        c.drawString(M, PAGE_H - 1.5 * inch, "J A V A   P R O J E C T   .   D E S I G N   G U I D E")
        c.setFont("Helvetica-Bold", 16); c.setFillColor(colors.HexColor("#CFE3EE"))
        c.drawString(M, PAGE_H - 2.1 * inch, "APPARAT")
        c.setFillColor(WHITE); c.setFont("Helvetica-Bold", 25)
        y = PAGE_H - 2.85 * inch
        for line in heading:
            c.drawString(M, y, line); y -= 0.42 * inch
        c.setFont("Helvetica", 12); c.setFillColor(colors.HexColor("#CFE3EE"))
        c.drawString(M, y - 0.05 * inch, tagline)
        c.setStrokeColor(TEAL); c.setLineWidth(1.4)
        c.line(M, y - 0.35 * inch, M + 2.3 * inch, y - 0.35 * inch)
        c.setFont("Helvetica-Bold", 14); c.setFillColor(WHITE)
        c.drawString(M, y - 0.85 * inch, owner)
        c.setFont("Helvetica", 10.5); c.setFillColor(colors.HexColor("#9FB6C7"))
        c.drawString(M, y - 1.1 * inch, roll)
        yy = y - 1.65 * inch
        c.setFont("Helvetica-Bold", 9.5); c.setFillColor(colors.HexColor("#9FD8C4"))
        c.drawString(M, yy, "WHAT THIS DOCUMENT GIVES YOU")
        c.setFont("Helvetica", 10); c.setFillColor(WHITE)
        for it in items:
            yy -= 0.24 * inch
            c.drawString(M + 0.12 * inch, yy, "-  " + it)
        c.setFont("Helvetica", 8.5); c.setFillColor(colors.HexColor("#7C93A5"))
        c.drawString(M, 0.7 * inch, "Every number in this guide was measured on the project (tests run, requests sent, files counted).")
        c.restoreState()
    return draw

COMMON_LAYER = flow(["Browser", "AuthFilter", "Servlet", "Service", "DAO + JDBC", "MySQL"],
                    [MUTED, TEAL, NAVY, NAVY, NAVY, colors.HexColor("#14304A")])

# =============================================================== NAZRIN
def build_nazrin():
    path = os.path.join(ROOT, "docs", "design", "DESIGN_Nazrin_WebInterface_Login.pdf")
    doc = make_doc(path, "Apparat design guide - Nazrin", "APPARAT  .  Design guide  .  Nazrin Nizzam (24BBS0111)  .  Web interface, login, sessions",
                   cover_factory("Nazrin Nizzam", "24BBS0111",
                                 ["Web Interface, Login", "and Sessions"],
                                 "Servlet / JSP, AuthFilter, availability page, shared layout",
                                 ["What was promised in Review 1 and what exists now",
                                  "How a request travels, and the login flow step by step",
                                  "The ideas behind each file, explained simply",
                                  "A demo script, the tests, and honest limits",
                                  "Fourteen likely viva questions with model answers"]))
    s = [NextPageTemplate("Body"), PageBreak()]

    s += H1("1. Your slice at a glance")
    s.append(P("In Review 1 your slice was <b>Web Interface and Servlet / JSP</b>. This table compares each promise with what the project contains today."))
    s.append(status_table([
        ("JSP pages", "Done", "login, home, availability, error pages added; resources, booking form and my bookings already existed"),
        ("Servlet request/response flow", "Done", "every page follows Browser > Filter > Servlet > Service > DAO > JSP"),
        ("Session-aware interface", "Done", "LoginServlet stores a SessionUser; the nav bar shows the name and role; links to staff and admin pages appear only for roles that hold the permission"),
        ("Form validation", "Done", "browser checks in js/app.js, then the servlet and service check again on the server"),
        ("UI design, CSS and JavaScript", "Done", "one shared css/app.css, one js/app.js, a shared nav fragment"),
        ("Web error presentation", "Done", "one error.jsp for 400, 403, 404, 405, 409, 500, registered in web.xml"),
        ("Real login for the whole site", "Done", "the three servlets that used a fixed user now read the logged-in user"),
        ("Registration, password change, remember-me", "Not built", "not in Review 1 either; do not claim them"),
    ]))

    s += H1("2. How a request travels")
    s.append(COMMON_LAYER)
    s.append(Spacer(1, 6))
    s += bullets([
        "<b>AuthFilter</b> runs before every servlet. It lets a few public pages through, sends strangers to the login page, and blocks staff-only pages for other roles.",
        "<b>Servlet</b> reads the request, calls one service method, then either forwards to a JSP (to show a page) or redirects (after a successful POST).",
        "<b>Service</b> holds the rules. <b>DAO</b> holds the SQL. The JSP never talks to the database.",
        "JSP files live under <font face='Courier'>WEB-INF/views</font>, so a browser cannot open them directly. Only a servlet can forward to them (verified: a direct request returns 404).",
    ])

    s += H1("3. The login flow, step by step")
    s.append(table(("#", "What happens", "File"), [
        ("1", "Visitor asks for any page and has no session. AuthFilter redirects to /login.", "filter/AuthFilter"),
        ("2", "LoginServlet (GET) shows the login form. The browser checks the email shape first.", "servlet/LoginServlet, js/app.js"),
        ("3", "Form is sent by POST. The servlet passes email and password to AuthService.", "servlet/LoginServlet"),
        ("4", "AuthService looks the email up (UserDao) and checks the password against the stored salted hash.", "service/AuthService, util/PasswordUtil"),
        ("5", "Wrong email or wrong password both give the same message, so nobody can learn which emails exist.", "service/AuthService"),
        ("6", "On success the servlet <b>invalidates any old session and creates a new one</b>, then stores a SessionUser (id, name, role).", "servlet/LoginServlet"),
        ("7", "Redirect to /home. Later requests carry the session cookie, and AuthFilter lets them through.", "servlet/HomeServlet"),
        ("8", "Logout is a POST. The session is invalidated, and the old cookie stops working.", "servlet/LogoutServlet"),
    ], [0.3 * inch, COL - 2.5 * inch, 2.2 * inch]))

    s += H1("4. Files you own")
    s.append(files_table([
        (JAVA + "filter/AuthFilter.java", "applies the access decision to every request; re-reads the account each time"),
        (JAVA + "filter/AccessRules.java", "which page needs which permission (pure logic, fully tested)"),
        (JAVA + "filter/SessionSupport.java", "stores and reads the logged-in user in the session"),
        (JAVA + "servlet/BaseServlet.java", "parent of servlets that need the logged-in user"),
        (JAVA + "servlet/LoginServlet.java", "login form and login check; creates the new session"),
        (JAVA + "servlet/LogoutServlet.java", "POST-only logout"),
        (JAVA + "servlet/HomeServlet.java", "landing page after login"),
        (JAVA + "servlet/AvailabilityServlet.java", "availability page for one instrument and day"),
        (JAVA + "service/AuthService.java", "login rules"),
        (JAVA + "service/AuthenticationException.java", "login failure type"),
        (JAVA + "service/AvailabilityService.java", "builds the 18-slot day grid"),
        (JAVA + "dao/UserDao.java", "reads a user and password hash by email"),
        (JAVA + "dao/AvailabilityDao.java", "reads booked slot times for one day"),
        (JAVA + "dto/SessionUser.java", "what the session remembers (never the hash)"),
        (JAVA + "dto/UserCredentials.java", "user row plus hash, used only during login"),
        (JAVA + "dto/SlotView.java", "one cell of the availability grid"),
        (JAVA + "util/PasswordUtil.java", "PBKDF2 hashing and checking"),
        (VIEWS + "login.jsp", "login page"),
        (VIEWS + "home.jsp", "home page"),
        (VIEWS + "availability.jsp", "availability page"),
        (VIEWS + "error.jsp", "friendly error page"),
        (VIEWS + "fragments/nav.jsp", "navigation bar shared by pages"),
        (WEB + "css/app.css", "shared styles, scoped by class"),
        (WEB + "js/app.js", "browser-side validation and confirmations"),
        (WEB + "WEB-INF/web.xml", "session timeout, error pages, welcome file"),
        (WEB + "index.jsp", "sends / to /home"),
    ]))

    s += H1("5. The ideas behind it, in plain words")
    ideas = [
        ("Authentication and authorization", "Authentication asks <i>who are you?</i> (the login). Authorization asks <i>what may you do?</i> (roles). Each page that needs a permission is listed in AccessRules, and Role.can(...) says which roles hold it. AuthFilter applies the decision; see the separate Roles and Permissions guide."),
        ("Session and cookie", "After login the server remembers you in an <b>HttpSession</b>. The browser holds only a random id in the JSESSIONID cookie. The cookie is marked HttpOnly (JavaScript cannot read it), and the session ends after 30 idle minutes (web.xml)."),
        ("Password hashing", "A password is never stored. We store <b>PBKDF2WithHmacSHA256</b> with a random <b>salt</b> and 120,000 iterations. The same password gives a different stored value every time, and the check uses a constant-time comparison."),
        ("Session fixation", "If the session id stayed the same across login, an attacker who knew it beforehand would be logged in too. So login throws the old session away and starts a new one. Measured: the id before and after login was different."),
        ("Same message for both failures", "Telling people 'no such email' would let someone test which emails exist. For an unknown email we still run a hash check, so the time taken is similar too."),
        ("Forward and redirect", "A <b>forward</b> hands the same request to a JSP inside the server (used to show pages). A <b>redirect</b> tells the browser to ask again (used after a successful POST, so pressing refresh cannot repeat the action)."),
        ("Why logout is a POST", "A link can be opened by accident or by another site. A POST needs a deliberate button press, and GET /logout returns 405 (measured)."),
        ("Escaping output (XSS)", "Every value shown on a page goes through <font face='Courier'>c:out</font>, which turns &lt; and &gt; into harmless text. A name like &lt;b&gt;x&lt;/b&gt; is shown as text, not run."),
        ("Browser checks versus server checks", "js/app.js gives quick feedback, but a browser can be bypassed, so the server checks everything again. The browser check is convenience; the server check is protection."),
        ("Filters and the interface idea", "AuthFilter <b>implements</b> the Jakarta <font face='Courier'>Filter</font> interface. Tomcat calls it before servlets. This is one real use of an interface in the project."),
    ]
    for t, d in ideas:
        s.append(KeepTogether([P(t, "h2"), P(d)]))

    s += H1("6. The availability page")
    s.append(P("Chooses an instrument and a date (default: today), then shows 18 half-hour rows from 09:00 to 18:00. A row is <b>Booked</b> if a row for that start time exists in <font face='Courier'>booking_slots</font>, and <b>Maintenance</b> if it falls inside a maintenance window (blocked for bookings). Cancelling a booking deletes its slot rows, so cancelled times show as <b>Free</b> again."))
    s.append(P("The grid is built by <font face='Courier'>AvailabilityService.buildGrid</font>, which touches no database, so unit tests can check it directly. Opening hours and slot length come from the constants in BookingService, so there is one place to change them."))

    s += H1("7. The booking page and its messages")
    s.append(P("<b>Choosing the time.</b> The old page used a free-typing date-time box, which accepted times the rules then rejected. The page now has a <b>date picker</b> and two <b>dropdown lists</b>, one for the start and one for the end, containing only valid half-hour times (09:00 to 17:30 for starts, 09:30 to 18:00 for ends). The lists are built by the servlet from the same opening hours and slot length the booking rules use, so there is one place to change them."))
    s += bullets([
        "Picking a start suggests an end 30 minutes later, and the end list only offers later times.",
        "Moving the start past the chosen end clears the end.",
        "For today, start times that have already passed are disabled. If none are left, the page says <i>No times are left today. Please choose another date.</i>",
        "A one-line summary appears, for example <i>Electroscope (PRP-711), Tuesday, 27 October 2026, 10:00 - 11:30 (1 h 30 min)</i>.",
        "<b>Check availability</b> opens the availability page for the same instrument and day.",
        "After a successful booking the user lands on <b>My bookings</b> with a one-time message, such as <i>Booking #6181 confirmed: Electroscope on 2026-11-25, 10:00 - 11:30.</i> (stored in the session and shown once, so a URL cannot fake it).",
        "If something fails, the form comes back with everything the user typed still filled in.",
    ])
    s.append(P("<b>Where each message comes from.</b> Rules live in the Service, so every entrance gets the same explanation. <font face='Courier'>BookingService.findRangeError</font> returns the reason a time range breaks a rule; <font face='Courier'>isValidRange</font> simply asks it, so the rules exist once. The servlet adds messages about the request itself."))
    s.append(table(("What went wrong", "Status", "What the user reads"), [
        ("No instrument / date / start / end chosen", "400", "Please choose an instrument. / a date. / a start time. / an end time."),
        ("Instrument id is not a number", "400", "Please choose an instrument from the list."),
        ("Date or time is not a real date or time", "400", "Please choose the date and times from the lists."),
        ("Start is in the past", "400", "That time has already passed. Please choose a later start time."),
        ("End is not after the start", "400", "The end time must be after the start time."),
        ("Shorter than 30 minutes", "400", "A booking must be at least 30 minutes long."),
        ("Start and end on different days", "400", "A booking must start and end on the same day."),
        ("Not on the hour or half hour", "400", "Bookings must start and end on the hour or half hour, for example 10:00 or 10:30."),
        ("Outside opening hours", "400", "The lab is open from 09:00 to 18:00. Please choose a time within these hours."),
        ("Instrument does not exist", "400", "That instrument does not exist."),
        ("Instrument is INACTIVE", "400", "This instrument is not available for booking right now."),
        ("Part of the time is already booked", "409", "That time is no longer free: someone else has booked part of it. Check the availability page and pick another time."),
        ("Time falls in a maintenance window", "409", "The instrument is under maintenance at that time. Check the availability page for free times."),
        ("Database failure", "500", "The friendly error page, with no technical details"),
    ], [2.0 * inch, 0.62 * inch, COL - 2.62 * inch]))
    s.append(P("The dropdowns make most of these impossible to cause from the page. The messages still matter, because a request can be sent without using the page, and the server must explain itself either way. The JavaScript shows the same wording before the form is sent.", "small"))

    s += H1("8. Small changes made to existing files")
    s.append(P("These are the only edits to code written earlier. Each is a few lines, and none changes behaviour except that the logged-in user is now used."))
    s.append(table(("File", "Change"), [
        ("BookingServlet, MyBookingsServlet, CancelBookingServlet", "the fixed <font face='Courier'>userId = 1</font> now comes from the session user; one import added to each"),
        ("resources.jsp, booking-form.jsp, my-bookings.jsp", "the shared stylesheet and nav bar added, their own 'body' width rule removed, and the content wrapped in the shared page column, so every page has the same layout (before this, each page had a different width)"),
        ("schema.sql (since removed)", "was extended for login and maintenance; the whole database is now built by <font face='Courier'>database/setup.sql</font>"),
        ("BookingService", "<font face='Courier'>findRangeError</font> added (the same rules, with a plain-words reason); the vague 'Invalid booking time range.' messages replaced; instrument messages reworded"),
        ("BookingServlet, booking-form.jsp", "reworked for the date and time dropdowns, specific messages, and the confirmation on My bookings"),
    ], [2.9 * inch, COL - 2.9 * inch]))
    s.append(P("The database is created by one file, <font face='Courier'>database/setup.sql</font>, which also adds the demo accounts below. The README explains how to run it. Tests use a separate database, apparat2_test.", "small"))

    s += H1("9. Demo script (six minutes)")
    s.append(P("Demo accounts are <b>sample data for development only</b>:"))
    s.append(table(("Email", "Password", "Role"), [
        ("student@apparat.test", "Student@123", "STUDENT"),
        ("faculty@apparat.test", "Faculty@123", "FACULTY"),
        ("tech@apparat.test", "Tech@123", "TECHNICIAN"),
        ("admin@apparat.test", "Admin@123", "ADMIN"),
    ], [2.4 * inch, 1.6 * inch, COL - 4.0 * inch]))
    s += bullets([
        "Open the site without logging in: you land on the login page.",
        "Try a wrong password: the message is the generic one. Log in as the student.",
        "Open Availability, pick an instrument and a day: all rows are Free.",
        "Open Book: pick an instrument and a date, then a start time. The end appears by itself and a summary shows the booking in words.",
        "Book 10:00-11:00 on that day. You land on My bookings with a confirmation. Reload Availability: two rows are now Booked.",
        "Log out, log in as faculty, try the same slot: you get the slot-taken message.",
        "As the student, try to open /manage/resources: a friendly 'Not allowed' page.",
        "Log in as the technician: the Manage and Reports links now appear.",
    ])

    s += H1("10. What was tested")
    s.append(table(("Test class", "Tests", "What it proves"), [
        ("PasswordUtilTests", "6", "right password verifies, wrong one does not; same password gives different hashes; garbage stored values never verify"),
        ("AuthServiceDbTests", "6", "success returns the session user; wrong password and unknown email give the same message; blank input and users with no password are refused (uses a real database)"),
        ("AvailabilityServiceTests", "7", "18 slots from 09:00 to 17:30; booked slots are marked; the closing time has no slot of its own; maintenance slots are marked separately from booked ones; a window covers every slot from its start to its end; windows on other days are ignored"),
        ("BookingMessageTests", "10", "every booking rule gives its own plain-words reason (missing, end not after start, too short, different days, off the half hour, outside hours); isValidRange agrees with the reasons; createBooking and generateSlots pass the reason on"),
        ("BookingInstrumentMessageDbTests", "2", "unknown instrument and INACTIVE instrument each get their own message (real database)"),
    ], [1.9 * inch, 0.55 * inch, COL - 2.45 * inch]))
    s.append(P("The whole project suite is now <b>154 tests, 0 failures</b> (24 existed before, 130 were added since)."))
    s.append(P("<b>Checks made against the running site</b> (Tomcat, real requests): unauthenticated visit redirects to login; wrong and unknown logins give 401 with the same text; the session id changes at login; a student gets 403 for /manage and /reports; a booking made while logged in is saved under the right user; a second user is refused (403) when cancelling someone else's booking; after logout the old cookie no longer works; GET /logout returns 405; an unknown page shows the custom 404 page; direct access to a JSP returns 404. <b>Layout check in a real browser:</b> on all eight pages opened from the dashboard (home, instruments, book, my bookings, availability, manage, maintenance, reports), at 1200 px and 700 px wide, the nav bar, the content column, the heading position and the Log out button had identical size and position, with no sideways scrolling. <b>Booking page, in the browser:</b> choosing a start suggested the end and limited the end list to later times; moving the start past the end cleared it; for today the passed times were disabled; each submit error showed the same wording as the server. <b>Booking page, over HTTP:</b> every message in the table above was triggered and read back, the success path showed its confirmation once, the form kept the typed values after an error, and a script typed into the date field was shown as harmless text."))

    s += H1("11. Honest limits (say these before the panel asks)")
    s += bullets([
        "<b>No CSRF tokens.</b> A malicious page could try to make a logged-in user's browser send a POST. A token per form would fix this; it is planned, not built.",
        "<b>No limit on login attempts</b>, so repeated guessing is not slowed down.",
        "<b>No registration or password change.</b> Users come from SQL (the demo file).",
        "<b>Roles are fixed in code</b> (an enum). Adding a fifth role is a code change, not something an administrator can do on a page.",
        "<b>The 'time has passed' rule is in the servlet and the browser, not in the Service.</b> Putting it in the Service would break the existing tests, which use fixed dates. Another entrance that calls the Service directly would not get this check.",
        "Availability is read when the page loads; it does not refresh by itself, and two people can still pick the same free slot (the booking step then reports the conflict).",
    ])

    s += H1("12. Viva questions with model answers")
    s += qa([
        ("What happens when I type a URL without being logged in?", "AuthFilter runs first. It finds no SessionUser in the session and redirects to /login. Only /login, /css and /js are open."),
        ("Why do you store a hash and not the password?", "If the database leaks, the passwords must not be readable. A salted PBKDF2 hash cannot be turned back into the password, and the salt makes equal passwords look different."),
        ("What is a salt, and why is it random?", "A salt is random data mixed into the hash. Because it is different for every user, an attacker cannot reuse one pre-computed table of hashes."),
        ("What is session fixation, and how do you prevent it?", "An attacker plants a known session id and waits for you to log in. We invalidate the old session on login and create a new one, so the planted id becomes useless."),
        ("Why is the error message the same for a wrong email and a wrong password?", "A different message would reveal which emails exist. We also verify against a dummy hash for unknown emails so response time does not give it away."),
        ("Forward or redirect after login, and why?", "Redirect. After a successful POST a redirect makes the browser do a fresh GET, so refreshing the page does not resend the login form."),
        ("Where is authorization checked?", "In AuthFilter for staff-only paths (/manage, /reports), and in the Service for data ownership (a user can only cancel their own booking)."),
        ("What stops a script in an instrument name from running?", "Every value is printed with c:out, which escapes HTML. The page shows the characters as text."),
        ("Why is logout a POST?", "So that opening a link, or another site, cannot log someone out. A GET to /logout returns 405."),
        ("Which Java interface does your code implement?", "AuthFilter implements jakarta.servlet.Filter. Tomcat calls its doFilter method before every request."),
        ("Does validation in JavaScript protect the system?", "No. It only helps the user. The servlet and service validate again, because anyone can send a request without using our page."),
        ("How does the booking page stop invalid times?", "It offers only valid half-hour times in dropdown lists, the end list only offers times after the start, and passed times are disabled for today. The servlet and the Service then check again, because a request can be sent without the page."),
        ("Why does each failure have its own message, and where do they come from?", "'Invalid booking' does not tell a user what to change. findRangeError in the Service returns the specific reason, so every entrance gets the same explanation; the servlet adds messages about the request, such as a missing field."),
        ("What would you add next?", "CSRF tokens on every POST form, a limit on login attempts, and password change. Each is a known gap, listed above."),
    ])
    doc.build(s)
    return path

# =============================================================== SHOURYA
def build_shourya():
    path = os.path.join(ROOT, "docs", "design", "DESIGN_Shourya_Resources_Reports.pdf")
    doc = make_doc(path, "Apparat design guide - Shourya", "APPARAT  .  Design guide  .  Shourya Goyal (24BBS0132)  .  Resources, validation, CSV, reports",
                   cover_factory("Shourya Goyal", "24BBS0132",
                                 ["Resources, Validation,", "CSV and Reports"],
                                 "Instrument management, file handling, utilization report",
                                 ["What was promised in Review 1 and what exists now",
                                  "How the management, import and report flows work",
                                  "File handling, CSV, collections and streams explained simply",
                                  "A demo script, the tests, and honest limits",
                                  "Fourteen likely viva questions with model answers"]))
    s = [NextPageTemplate("Body"), PageBreak()]

    s += H1("1. Your slice at a glance")
    s.append(P("In Review 1 your slice was <b>Resources, Validation and Reports</b>. This table compares each promise with what the project contains today."))
    s.append(status_table([
        ("Resource management", "Done", "add, rename, change location, activate and deactivate (technicians and administrators)"),
        ("Resource status", "Done", "ACTIVE / INACTIVE; inactive instruments are hidden from the booking form"),
        ("Maintenance windows and maintenance UI", "Done", "technician page /manage/maintenance; windows block bookings for a time period; the availability page shows them as Maintenance"),
        ("File handling", "Done", "CSV import (upload, read line by line) and CSV downloads (report and my bookings)"),
        ("CSV import / export", "Done", "ImportResourcesServlet, ExportBookingsServlet, ReportServlet?format=csv"),
        ("Report generation", "Done", "utilization report per instrument over a date range, as a page and a CSV file"),
        ("Input validation", "Done", "four layers: browser, servlet, service, model constructor"),
        ("Exception handling and testing", "Done", "typed exceptions mapped to HTTP codes; 38 new tests for this slice, including a concurrency test"),
    ]))

    s += H1("2. The three flows")
    s.append(H2("Managing instruments (add, rename, change status)"))
    s.append(flow(["Browser<br/>form", "AuthFilter<br/>(role check)", "Manage<br/>Resources<br/>Servlet", "Resource<br/>Admin<br/>Service", "Resource<br/>Admin<br/>Dao", "MySQL"],
                  [MUTED, TEAL, NAVY, NAVY, NAVY, colors.HexColor("#14304A")]))
    s.append(Spacer(1, 5))
    s.append(H2("Importing instruments from CSV"))
    s.append(flow(["Choose<br/>.csv file", "Import<br/>Resources<br/>Servlet", "Read lines<br/>(UTF-8)", "Check<br/>each row", "Save valid<br/>rows in ONE<br/>transaction", "Summary<br/>message"],
                  [MUTED, NAVY, NAVY, NAVY, colors.HexColor("#14304A"), GREEN]))
    s.append(Spacer(1, 5))
    s.append(H2("Report and downloads"))
    s.append(flow(["Pick<br/>dates", "Report<br/>Servlet", "Report<br/>Service", "Report Dao<br/>(JOIN query)", "Page or<br/>CSV file"],
                  [MUTED, NAVY, NAVY, colors.HexColor("#14304A"), GREEN]))
    s.append(Spacer(1, 5))
    s.append(H2("Maintenance windows"))
    s.append(flow(["Technician<br/>form", "AuthFilter<br/>(role check)", "Manage<br/>Maintenance<br/>Servlet", "Maintenance<br/>Service<br/>(lock + checks)", "Maintenance<br/>Dao", "MySQL"],
                  [MUTED, TEAL, NAVY, colors.HexColor("#14304A"), NAVY, colors.HexColor("#14304A")]))
    s.append(Spacer(1, 4))
    s.append(P("All four pass through <b>AuthFilter</b>: /manage and /reports need staff permissions (technician or administrator), so a student gets a 403 page (measured)."))

    s += H1("3. Files you own")
    s.append(files_table([
        (JAVA + "servlet/ManageResourcesServlet.java", "list, add, update, status change; one-time messages"),
        (JAVA + "servlet/ImportResourcesServlet.java", "receives the CSV upload and reads it"),
        (JAVA + "servlet/ReportServlet.java", "report page and CSV download"),
        (JAVA + "servlet/ExportBookingsServlet.java", "downloads the user's own bookings as CSV"),
        (JAVA + "servlet/ManageMaintenanceServlet.java", "list, add and remove maintenance windows"),
        (JAVA + "service/MaintenanceService.java", "maintenance rules: validation, lock, overlap checks"),
        (JAVA + "service/MaintenanceConflictException.java", "window clashes with a booking or another window"),
        (JAVA + "service/MaintenanceNotFoundException.java", "unknown window id"),
        (JAVA + "dao/MaintenanceDao.java", "insert, delete, overlap queries"),
        (JAVA + "model/MaintenanceWindow.java", "a window; the constructor rejects bad values"),
        (JAVA + "dto/MaintenanceView.java", "a window with the instrument name, for the page"),
        (VIEWS + "manage-maintenance.jsp", "maintenance page"),
        (JAVA + "service/ResourceAdminService.java", "all validation, duplicate checks and import logic"),
        (JAVA + "service/ReportService.java", "turns slot counts into hours and percentages"),
        (JAVA + "service/ResourceNotFoundException.java", "unknown instrument id"),
        (JAVA + "service/DuplicateResourceException.java", "name already used"),
        (JAVA + "dao/ResourceAdminDao.java", "insert, update, status update"),
        (JAVA + "dao/ReportDao.java", "usage counts per instrument"),
        (JAVA + "dto/ImportResult.java", "rows imported plus one message per rejected row"),
        (JAVA + "dto/UsageCount.java", "raw counts from the database"),
        (JAVA + "dto/UtilizationRow.java", "one finished report line"),
        (JAVA + "util/CsvUtil.java", "writes and parses CSV safely"),
        (VIEWS + "manage-resources.jsp", "management page with add, import and edit forms"),
        (VIEWS + "report.jsp", "report page"),
    ]))

    s += H1("4. File handling, explained simply")
    ideas = [
        ("Reading an upload", "The browser sends the file inside the POST request (<font face='Courier'>multipart/form-data</font>). The servlet is marked <font face='Courier'>@MultipartConfig</font> with a 1 MB limit, and gets the file with <font face='Courier'>request.getPart(\"file\")</font>. It must be non-empty and its name must end in .csv."),
        ("Reading line by line", "A <font face='Courier'>BufferedReader</font> wraps an <font face='Courier'>InputStreamReader</font> that decodes the bytes as UTF-8. <font face='Courier'>readLine()</font> returns one line at a time, so even a large file never has to sit in memory as one block."),
        ("try-with-resources", "The reader is opened in the try header, so it is closed automatically, even if an exception is thrown halfway through the file."),
        ("Writing a download", "The servlet sets <font face='Courier'>Content-Type: text/csv</font> and <font face='Courier'>Content-Disposition: attachment</font>, so the browser saves a file instead of showing text. It then prints lines with a <font face='Courier'>PrintWriter</font>."),
        ("Byte-order mark", "Excel sometimes puts an invisible character at the start of a UTF-8 file. The importer removes it, so the first column name is read correctly (tested)."),
    ]
    for t, d in ideas:
        s.append(KeepTogether([P(t, "h2"), P(d)]))

    s += H1("5. CSV rules")
    s.append(P("Format: <font face='Courier'>name,location[,status]</font>. A header line is allowed. Status is optional (ACTIVE or INACTIVE, any letter case; default ACTIVE). Maximum 500 rows."))
    s.append(P("Example, and what the real run produced:", "small"))
    s.append(code('name,location,status\nZZ-VERIFY-Csv1,Bench 1\nZZ-VERIFY-Csv2,Bench 2,INACTIVE\nZZ-VERIFY-Csv1,Bench 3          <- duplicate name, rejected\n"ZZ-VERIFY-Csv3, quoted",Bench 4  <- comma inside quotes, accepted\nbroken-line                     <- only one column, rejected'))
    s.append(P("Result shown on the page: <b>Imported 3 instrument(s); 2 row(s) rejected</b>, followed by the two rejected line numbers with reasons."))
    s += bullets([
        "<b>Quoting.</b> A value containing a comma or quote is wrapped in quotes, and a quote inside is doubled. <font face='Courier'>CsvUtil</font> does this when writing and understands it when reading. Writing then reading gives back the original values (tested).",
        "<b>CSV injection.</b> A cell starting with = + - or @ can run as a formula when the file is opened in a spreadsheet. <font face='Courier'>CsvUtil.escape</font> puts a quote in front of such cells, so an instrument named =HYPERLINK(...) is exported as harmless text.",
        "<b>Duplicates.</b> Names are compared ignoring case, against instruments already stored <i>and</i> against earlier rows in the same file. The check uses a <font face='Courier'>Set</font>, which makes 'have I seen this name?' one quick lookup.",
        "<b>All valid rows or none of them.</b> Valid rows are inserted in <b>one transaction</b>. If the database fails halfway, everything is rolled back. Rejected rows never reach the database.",
    ])

    s += H1("6. The utilization report")
    s.append(P("<b>Definition:</b> utilization = booked hours / open hours in the range. Open hours = number of days (both end dates included) x 9 hours (09:00 to 18:00). Booked hours = number of booked 30-minute slots x 0.5."))
    s.append(P("Worked example from the tests: one 10:00-11:00 booking (2 slots) on a single day gives 1.0 booked hour out of 9, which is <b>11.1 percent</b>. Over a two-day range it is 1 out of 18, which is <b>5.6 percent</b>."))
    s.append(P("The data comes from one SQL query. <font face='Courier'>resources LEFT JOIN booking_slots</font> keeps instruments that have no bookings (they show 0), and <font face='Courier'>COUNT(DISTINCT booking_id)</font> counts bookings while <font face='Courier'>COUNT(slot_start)</font> counts slots."))
    s.append(P("<b>Why cancelled bookings do not count:</b> cancelling deletes a booking's slot rows, so they are simply not there to be counted. Nothing special is needed in the report."))
    s.append(P("The service uses a <b>stream</b> to turn each database row into a report row, and the servlet uses streams to add up the footer totals."))

    s += H1("7. Maintenance windows")
    s.append(P("<b>What it is:</b> a time period when an instrument cannot be booked, for example 10:00 to 11:00 for a lamp change. A booking that overlaps a window is refused with a clear message, and the availability page shows those slots as <b>Maintenance</b>. Long outages still use the INACTIVE switch."))
    s.append(P("<b>Rules:</b> a window follows the booking rules (one day, start and end on the half hour, inside 09:00 to 18:00, at least 30 minutes) and needs a reason of at most 200 characters. A window is refused if it overlaps another window, or if a <b>confirmed</b> booking already overlaps it (cancelled bookings do not count). Nothing is cancelled automatically."))
    s.append(P("<b>Overlap test:</b> two periods overlap when each starts before the other ends: <font face='Courier'>existing.start &lt; new.end AND existing.end &gt; new.start</font>. A window ending at 11:00 and a booking starting at 11:00 do not overlap (tested)."))
    s.append(P("<b>Why not reuse the booking_slots table?</b> Its <font face='Courier'>booking_id</font> column is required and points to a booking, so a window cannot live there without changing that design."))
    s.append(P("<b>How a booking and a window are kept apart:</b> both <font face='Courier'>createBooking</font> and <font face='Courier'>addWindow</font> start a transaction, lock the instrument's row with <font face='Courier'>SELECT ... FOR UPDATE</font>, and only then check for overlaps. Only one of them can hold that lock at a time, so they take turns, and whoever goes second sees the other's result. This is the same lock that already removed the deadlocks in the booking stress test."))
    s.append(P("<b>Proof:</b> a test starts one thread adding a window and 29 threads booking overlapping times, and repeats that for 8 rounds. In the recorded run, the bookings won 6 rounds (the window was refused) and the window won 2 rounds (every booking was refused); the split changes from run to run. In every round a window and a booking never coexisted, and the only failures were the clean, expected types (no deadlocks). Removing the check from <font face='Courier'>createBooking</font> made this test fail, so it is a real guard."))
    s.append(P("<b>Small changes to code written earlier:</b> BookingService got one lookup and a four-line check after the 'instrument is active' check, and BookingServlet got one extra catch (409 with the maintenance message). database/setup.sql contains the new table.", "small"))

    s += H1("8. Exceptions and HTTP codes")
    s.append(table(("Situation", "Exception", "Status", "User sees"), [
        ("Name already used", "DuplicateResourceException", "409", "that name already exists"),
        ("Window or booking overlaps the time", "MaintenanceConflictException", "409", "the specific message"),
        ("Unknown instrument or window id", "ResourceNotFoundException / MaintenanceNotFoundException", "404", "does not exist"),
        ("Blank, too long, or malformed value", "IllegalArgumentException", "400", "the specific message"),
        ("id or date is not a number or date", "NumberFormatException / DateTimeParseException", "400", "please check your inputs"),
        ("Wrong file type, empty file, file over 1 MB", "(checked in the servlet)", "400", "message on the error page"),
        ("Database failure", "SQLException", "500", "generic error page, no details"),
    ], [1.8 * inch, 2.0 * inch, 0.55 * inch, COL - 4.35 * inch]))

    s += H1("9. Validation: four layers")
    s.append(table(("Layer", "What it checks", "Why it is there"), [
        ("Browser (js/app.js)", "required fields, length, .csv extension, 1 MB, start date not after end date", "instant feedback"),
        ("Servlet", "action name, numeric ids, file present and named .csv", "the request shape"),
        ("Service", "trimmed values, length 100, duplicate names, instrument exists, date range at most 366 days", "business rules"),
        ("Model (Resource constructor)", "name, location and status not blank or null", "an invalid object cannot exist"),
    ], [1.7 * inch, 3.0 * inch, COL - 4.7 * inch]))

    s += H1("10. Demo script (six minutes)")
    s += bullets([
        "Log in as <b>tech@apparat.test</b> (password <b>Tech@123</b>, sample data) and open <b>Manage</b>.",
        "Add an instrument. Add the same name in capitals: the duplicate message appears.",
        "Rename it, then deactivate it, and open the booking form as a student: the instrument is not offered.",
        "Import a small CSV with one good row and one bad row: the summary lists the rejected line.",
        "Open <b>Reports</b>, choose a range, then press <b>Download CSV</b> and open the file.",
        "Open <b>Maintenance</b>, add a window 10:00-11:00 for an instrument with a reason.",
        "As a student, try to book 10:30-11:30 on that day: the booking is refused with the maintenance message. Open Availability: those slots show Maintenance.",
        "Log in as a student and try /reports: a friendly 'Not allowed' page.",
    ])

    s += H1("11. What was tested")
    s.append(table(("Test class", "Tests", "What it proves"), [
        ("CsvUtilTests", "9", "quoting, doubled quotes, formula neutralising, parsing of quoted fields, empty fields, missing closing quote, round trip"),
        ("ResourceAdminServiceDbTests", "11", "add, duplicates ignoring case, blank and over-long values, rename, unknown id, status switch both ways, import with good and bad rows, existing names skipped, byte-order mark (real database, cleaned up afterwards)"),
        ("ReportServiceDbTests", "4", "11.1 and 5.6 percent examples, zero rows for empty ranges, invalid ranges refused (creates a real booking, then removes it)"),
        ("MaintenanceServiceDbTests", "13", "window saved and listed; booking inside or around a window refused with nothing saved; bookings that only touch the window allowed; window over a confirmed booking refused; cancelled bookings do not block; overlapping windows refused, adjacent ones allowed; invalid times and reasons; unknown instrument; removing a window reopens the time; unknown window; availability shows maintenance, booked and free slots"),
        ("MaintenanceConcurrencyTests", "1", "8 rounds of one window against 29 bookings: a window and a booking never coexist, only expected failure types"),
    ], [2.0 * inch, 0.55 * inch, COL - 2.55 * inch]))
    s.append(P("The whole project suite is now <b>154 tests, 0 failures</b>. As a check that the new tests can fail, five deliberate bugs were put into the code one at a time (no duplicate check in the import, a wrong capacity in the report, a password check that always passes, bookings ignoring maintenance windows, windows ignoring existing bookings). Each was caught by the matching tests, and the code was then restored."))
    s.append(P("<b>Checks made against the running site:</b> adding, duplicate (409), blank (400), renaming, deactivating (and the booking form hiding it), unknown id (404), a name containing HTML shown as escaped text, a CSV import with 3 good and 2 bad rows, a .txt upload (400), a missing file (400), a 1.5 MB upload (400), the report page, the CSV download with the right headers, a reversed date range (400), and a student refused (403). For maintenance: adding a window, a booking inside it refused (409, nothing saved), availability marking two Maintenance rows, a window over an existing booking refused, an overlapping window refused, off-grid time and blank reason refused (400), removing a window and removing it again (404)."))

    s += H1("12. Honest limits (say these before the panel asks)")
    s += bullets([
        "<b>Maintenance windows cover part of one day only</b> (same rules as bookings). A longer outage uses the INACTIVE switch.",
        "<b>Adding a window never cancels bookings.</b> If confirmed bookings overlap, the window is refused and staff must cancel them first.",
        "A window cannot be edited; remove it and add a new one. Windows in the past are not rejected, because bookings have no 'future only' rule either.",
        "<b>Duplicate names are checked in Java only.</b> The table has no unique constraint on the name, so two staff members adding the same name at the exact same moment could both succeed. A unique constraint would close this; it is not added yet.",
        "Deactivating an instrument does not cancel bookings that already exist for it.",
        "Import has no preview step: valid rows are saved immediately.",
        "The report counts confirmed bookings through their slots; it has no charts and no per-user breakdown.",
        "Staff pages are open to technicians and administrators alike; there is no way to give one person only part of them.",
    ])

    s += H1("13. Viva questions with model answers")
    s += qa([
        ("How does the import read a file?", "The servlet gets the uploaded part, wraps its input stream in an InputStreamReader (UTF-8) and a BufferedReader, and reads one line at a time inside try-with-resources, so the file is always closed."),
        ("Why try-with-resources?", "It closes the file automatically when the block ends, even if an exception is thrown. Forgetting to close a file leaks a resource."),
        ("What happens to bad rows in an import?", "Each bad row is skipped and reported with its line number and reason. Valid rows are saved together in one transaction, so the database never ends up half imported."),
        ("Why a transaction for the import?", "To make saving the valid rows all-or-nothing. If row 40 fails in the database, rows 1 to 39 are rolled back too."),
        ("What is CSV injection and how do you prevent it?", "A cell beginning with =, +, - or @ can run as a formula in a spreadsheet. CsvUtil.escape puts a quote before such a cell so it is read as text."),
        ("Why use a Set for duplicate names?", "A set answers 'have I seen this?' with one lookup, and add() tells us in the same step whether the name was new. A list would need a scan for every row."),
        ("How is utilization calculated?", "Booked hours (slots x 0.5) divided by open hours (days x 9). One booked hour in one day is 1/9, which is 11.1 percent."),
        ("Why does the report use a LEFT JOIN?", "So an instrument with no bookings still appears, with zeros. An inner join would silently drop it."),
        ("Why do cancelled bookings not count?", "Cancelling deletes the booking's slot rows, and the report counts slot rows."),
        ("Where is validation done, and why in several places?", "In the browser (for speed), the servlet, the service (business rules) and the model constructor. The browser can be bypassed, so the server layers are the real protection."),
        ("Which exception becomes which HTTP code?", "Duplicate name 409, unknown instrument 404, bad input 400, database failure 500. Each type lets the servlet choose the right response."),
        ("How do you stop a booking and a maintenance window being saved for the same time?", "Both transactions lock the instrument's row with SELECT ... FOR UPDATE and check for overlaps only after getting that lock, so they take turns. A test races one window against 29 bookings for 8 rounds, and a window and a booking never coexisted."),
        ("Why was the booking_slots table not reused for maintenance?", "Its booking_id column is required and refers to a booking, so a maintenance window would need a different table design. A separate table plus the instrument lock gives the same safety."),
        ("What is the weakest part of this slice?", "Duplicate instrument names are only checked in Java, so a unique constraint on the name is the fix. Maintenance windows are limited to one day and cannot be edited."),
    ])
    doc.build(s)
    return path


# =============================================================== ROLES (shared)
def build_roles():
    path = os.path.join(ROOT, "docs", "design", "DESIGN_Roles_and_Permissions.pdf")
    doc = make_doc(path, "Apparat design guide - Roles and permissions", "APPARAT  .  Design guide  .  Roles and permissions  .  whole team",
                   cover_factory("Whole team", "Shared guide: read this before the viva",
                                 ["Roles and", "Permissions"],
                                 "Four roles, one permission table, booking limits, user management",
                                 ["Who may do what, in one table",
                                  "How a request is checked, step by step",
                                  "Booking limits per role, and the race we found and fixed",
                                  "Safety rules for administrators, and what was tested",
                                  "Fourteen likely viva questions with model answers"]))
    s = [NextPageTemplate("Body"), PageBreak()]

    s += H1("1. Who may do what")
    s.append(P("Apparat has four roles. Everything below is decided in <b>one place</b> in the code: the <font face='Courier'>Role</font> enum, which lists the permissions each role holds. Pages, menus and services all ask <font face='Courier'>Role.can(...)</font>; none of them compares role names."))
    s.append(table(("", "Student", "Faculty", "Technician", "Admin"), [
        ("Browse, check availability, book, cancel own bookings", "yes", "yes", "yes", "yes"),
        ("Longest single booking", "2 h", "4 h", "6 h", "no limit"),
        ("Confirmed bookings held at once", "3", "6", "10", "no limit"),
        ("Manage instruments, import CSV", "-", "-", "yes", "yes"),
        ("Maintenance windows", "-", "-", "yes", "yes"),
        ("Utilization report and CSV", "-", "-", "yes", "yes"),
        ("Create users, change roles, reset passwords, deactivate", "-", "-", "-", "yes"),
    ], [2.6 * inch, 0.8 * inch, 0.8 * inch, 0.85 * inch, COL - 5.05 * inch]))
    s.append(P("The four permissions are <font face='Courier'>MANAGE_RESOURCES</font>, <font face='Courier'>MANAGE_MAINTENANCE</font>, <font face='Courier'>VIEW_REPORTS</font> and <font face='Courier'>MANAGE_USERS</font>. A technician holds the first three; an administrator holds all four; students and faculty hold none. The booking limits are the named constants in the four policy classes, so changing a number is a one-line edit.", "small"))

    s += H1("2. How a request is checked")
    s.append(flow(["Browser", "AuthFilter<br/>(re-reads the account)", "AccessRules<br/>(page needs?)", "Servlet", "Service<br/>(role limits)", "MySQL"],
                  [MUTED, TEAL, TEAL, NAVY, NAVY, colors.HexColor("#14304A")]))
    s.append(Spacer(1, 6))
    s.append(table(("Step", "What happens", "Where"), [
        ("1", "The filter reads the logged-in user from the session. For every page it then <b>re-reads the account from the database</b>, so a role change, a deactivation or a deletion applies on the user's very next click.", "filter/AuthFilter, service/AuthService.refresh"),
        ("2", "AccessRules decides: public page (allow), nobody logged in (send to login), user lacks the needed permission (403), otherwise allow.", "filter/AccessRules"),
        ("3", "Unlisted pages under /manage and /admin need the administrator's permission, so a new page added there is <b>closed until someone opens it on purpose</b>.", "filter/AccessRules"),
        ("4", "The servlet asks BaseServlet who is logged in, and calls the service.", "servlet/BaseServlet"),
        ("5", "Inside the booking transaction the service looks up the user's role and applies that role's limits.", "service/BookingService, policy/"),
        ("6", "Menus and the home page show only what the role may use, from a permissions map stored in the session.", "views/fragments/nav.jsp, home.jsp"),
    ], [0.45 * inch, COL - 2.65 * inch, 2.2 * inch]))

    s += H1("3. Files")
    s.append(files_table([
        (JAVA + "model/Role.java", "the four roles and the permissions each holds"),
        (JAVA + "model/Permission.java", "the list of permissions"),
        (JAVA + "filter/AccessRules.java", "which page needs which permission"),
        (JAVA + "filter/AuthFilter.java", "applies the decision on every request"),
        (JAVA + "filter/SessionSupport.java", "session user and permissions map"),
        (JAVA + "servlet/BaseServlet.java", "parent servlet that knows the logged-in user"),
        (JAVA + "policy/BookingPolicy.java", "abstract class: the limit checks and their messages"),
        (JAVA + "policy/StudentPolicy.java", "2 hours, 3 active"),
        (JAVA + "policy/FacultyPolicy.java", "4 hours, 6 active"),
        (JAVA + "policy/TechnicianPolicy.java", "6 hours, 10 active"),
        (JAVA + "policy/AdminPolicy.java", "no limits"),
        (JAVA + "service/BookingLimitException.java", "a limit was reached"),
        (JAVA + "dao/BookingCountDao.java", "counts a user's active bookings"),
        (JAVA + "service/UserAdminService.java", "user management and its safety rules"),
        (JAVA + "dao/UserAdminDao.java", "user writes and the administrator lock"),
        (JAVA + "servlet/AdminUsersServlet.java", "the Users page"),
        (JAVA + "servlet/HomeServlet.java", "the role-aware home page"),
        (VIEWS + "manage-users.jsp", "Users page"),
        (VIEWS + "home.jsp", "home page"),
        (VIEWS + "fragments/nav.jsp", "menu, permission-aware"),
        ("database/setup.sql", "builds the whole database, with demo accounts"),
    ]))

    s += H1("4. Booking limits: inheritance and polymorphism with a real job")
    s.append(P("<font face='Courier'>BookingPolicy</font> is an <b>abstract class</b>. It contains the checking logic once (is this booking too long? does the user already hold too many?) and the message wording. Each role is a small <b>subclass</b> that only supplies two numbers by overriding <font face='Courier'>maxMinutesPerBooking()</font> and <font face='Courier'>maxActiveBookings()</font>. Calling the same method on a different policy object gives a different answer; that is <b>polymorphism</b>. A factory method, <font face='Courier'>BookingPolicy.forRole(role)</font>, picks the right subclass."))
    s.append(P("<b>What counts as active:</b> a confirmed booking that has not finished yet. Cancelled and finished bookings do not count, so cancelling frees a place."))
    s.append(P("<b>Order of the checks:</b> the booking length is checked before the count, so a user who breaks both limits hears about the booking they are making. Every message says what to do next."))

    s.append(H2("The race the tests caught (worth telling in the viva)"))
    s.append(P("A student tried to book eight different instruments at the same instant, with a limit of three. The first version let <b>all eight</b> through. Each instrument has its own lock, so those requests never wait for each other, and I added a lock on the <b>user's row</b> to make one person's bookings take turns. That alone was not enough."))
    s.append(P("<b>The cause:</b> MySQL's default isolation level gives a transaction a <i>snapshot</i> of the data, taken at its first plain SELECT. The maintenance check, a plain SELECT, ran <i>before</i> the transaction got the user lock. After waiting for the lock, each request counted bookings from its old snapshot and saw none. The lock worked; the count was stale."))
    s.append(P("<b>The fix:</b> count the active bookings with a <b>locking read</b> (<font face='Courier'>SELECT ... FOR UPDATE</font>), which always reads the latest saved data. After the fix, 18 rounds over three runs produced exactly 3 successes out of 8 every time, and the whole suite showed 0 deadlocks. Putting the plain SELECT back made the test fail again (8 of 8), so the test is a real guard."))

    s += H1("5. User management and its safety rules")
    s.append(P("Administrators have a Users page. Every action goes through <font face='Courier'>UserAdminService</font>, which checks the rules before touching the database."))
    s.append(table(("Action", "Rules and messages"), [
        ("Create a user", "name required and at most 100 characters; a valid email shape; a role from the list; password of at least 8 characters with a letter and a digit. A repeated email (any letter case) is refused: <i>A user with this email already exists.</i>"),
        ("Change a role", "<i>You cannot change your own role.</i> Demoting the last active administrator is refused: <i>There must always be at least one active administrator.</i>"),
        ("Deactivate or activate", "<i>You cannot deactivate your own account.</i> The same last-administrator rule applies. A deactivated user is logged out on their next click."),
        ("Reset a password", "same password rules; the old password stops working at once. Passwords are stored only as salted hashes, and are never shown or sent back to the page."),
    ], [1.5 * inch, COL - 1.5 * inch]))
    s.append(P("<b>Login for deactivated accounts:</b> the message <i>This account has been deactivated</i> is shown only after the right password was entered, so it cannot be used to find out which emails exist. A wrong password still gets the same generic message as always."))
    s.append(P("<b>Why administrators are locked first:</b> two administrators could demote each other at the same instant, and each could check 'another administrator exists' before either change was saved, leaving none. Every change that could remove an administrator first locks all active administrator rows, so the changes take turns. A test runs exactly that race, 8 rounds: one succeeds, one is refused, and one administrator always remains."))

    s += H1("6. The home page by role")
    s.append(P("The home page greets the user by time of day, shows the badge and the limits of their role in one sentence, and three tiles: active bookings (for example '1 of 3'), the next booking, and instruments available. Below are the next bookings, or a friendly message when there are none."))
    s += bullets([
        "<b>Technicians and administrators</b> also see the next maintenance windows, with links to manage instruments and reports.",
        "<b>Administrators</b> also see active accounts per role and how many are deactivated.",
        "A student's page has none of those panels, and the menu hides the matching links.",
    ])

    s += H1("7. Housekeeping done at the same time")
    s += bullets([
        "<b>One setup file.</b> database/setup.sql builds every table and the demo accounts; database/setup-db.sh creates both databases and runs it, after asking for confirmation because it drops tables.",
        "<b>A separate test database.</b> mvn test runs against apparat2_test, so the tests never touch the data used for demos. A guard stops any test aimed at a database whose name does not end in _test, with a message that says how to fix it. Measured: a full test run left the demo database's rows and id counters exactly as they were.",
        "<b>Tidy-up.</b> The scratch test classes and the hello servlet were removed, and the repeated 'who is logged in' code in five servlets became one BaseServlet method.",
        "<b>Tables scroll inside their own box</b> on narrow screens; before, wide tables pushed the whole page sideways.",
    ])

    s += H1("8. What was tested")
    s.append(table(("Test class", "Tests", "What it proves"), [
        ("RolePermissionTests", "7", "students and faculty hold nothing; technician holds three; admin holds all four and is a superset of technician; only admin manages users; the permission set cannot be changed from outside"),
        ("AccessRulesTests", "3", "one table: every role against 21 URLs plus a visitor who is not logged in; unlisted /manage and /admin pages stay closed; a path that merely starts with 'manage' is not treated as one; the role decides, not the name"),
        ("BookingPolicyTests", "11", "each role's numbers, exact-limit bookings allowed, messages worded as in the booking page, length checked before count, admin unlimited"),
        ("BookingPolicyDbTests", "11", "limits enforced through createBooking against the real database; a refused booking leaves nothing behind; cancelling frees a place; finished bookings do not count; one user's limit does not affect another"),
        ("BookingLimitConcurrencyTests", "1", "eight simultaneous bookings by one student: exactly three succeed, over 6 rounds"),
        ("AccountStateDbTests", "7", "a role change, a deactivation and a deletion are all seen by the next refresh; a deactivated user cannot log in; reactivating works"),
        ("UserAdminServiceDbTests", "19", "every create validation message; duplicate email; role and active rules; last-administrator rule; password reset; two administrators demoting each other"),
        ("HomeServletTests", "2", "the greeting and the time wording"),
    ], [2.1 * inch, 0.55 * inch, COL - 2.65 * inch]))
    s.append(P("The whole project suite is now <b>154 tests, 0 failures</b>. Deliberate bugs, each restored afterwards: opening unlisted /manage pages to everyone, removing the last-administrator rule, and putting the plain SELECT back in the booking count. Each was caught by the matching tests."))
    s.append(P("<b>Checks made against the running site</b> (on the test database): every role against 12 pages (200, 403 or sent to login, as designed), the menu links per role, the home page per role, the role limits through the booking form, and the whole user-management flow: creating a user, a duplicate and weak password refused, the new user logging in, a promotion and a demotion applying on the same open session's next page, a deactivation logging that session out, the deactivated-account message, a password reset, and a student or technician trying the admin action directly (403, nothing changed)."))

    s += H1("9. Honest limits (say these before the panel asks)")
    s += bullets([
        "<b>No separate permission for each admin action.</b> An administrator holds all four permissions; there is no 'user manager but not reports' role.",
        "<b>Roles are fixed in code.</b> A new role needs a code change and a new policy class.",
        "<b>Limits are constants,</b> not settings an administrator can edit on a page.",
        "<b>The role is read on every page request,</b> which costs one small query per page. That is acceptable at this size; a busy site would cache it.",
        "No approval workflow, no CSRF tokens on forms, no limit on login attempts, and no self-service password change.",
        "The 'time has already passed' rule is checked in the booking page, not in the service.",
    ])

    s += H1("10. Viva questions with model answers")
    s += qa([
        ("How are roles separated in your project?", "In one place: the Role enum lists the permissions each role holds. AccessRules maps each page to a permission, AuthFilter applies it, and the menus and services ask Role.can(...) instead of comparing role names."),
        ("What happens if someone adds a new page under /manage and forgets to protect it?", "It stays closed. Unlisted pages under /manage and /admin need the administrator's permission, so a mistake fails safe. A test checks this."),
        ("Where is polymorphism in your project?", "BookingPolicy.forRole returns a different subclass per role, and createBooking calls the same check method on whichever it gets. Each subclass overrides the two limit methods, so the same call gives a different answer per role."),
        ("Why is BookingPolicy an abstract class and not an interface?", "The checking logic and messages are shared by every role, so they are written once in a base class. Subclasses only supply their numbers. An interface would force each role to repeat the logic."),
        ("Why do you re-read the user's role on every request?", "Otherwise a role change or deactivation would not apply until the user logged out. Re-reading means an administrator's change takes effect on the user's next click."),
        ("What stops the system having no administrator?", "Changes that could remove an administrator first lock all active administrator rows, then check that another one remains. Nobody can change or deactivate their own account. A test races two administrators demoting each other."),
        ("Explain the bug you found in the booking limit.", "MySQL gives a transaction a snapshot at its first plain SELECT. A plain count after waiting for the lock still used the old snapshot, so eight parallel bookings all passed a limit of three. A locking read (FOR UPDATE) reads the latest data, and the test now gives exactly three."),
        ("Why lock the user's row and not just the instrument?", "Different instruments have different locks, so one person booking several instruments at once would never wait. The user lock makes one person's bookings take turns."),
        ("Why can a deactivated user not use an old session?", "AuthFilter re-reads the account on every page; if it is deactivated or deleted, the session is ended and the user is sent to the login page."),
        ("Why does the deactivated-account message appear only after the right password?", "So an attacker cannot learn which emails exist from the message. Without the right password the answer is the same generic one as for any wrong login."),
        ("How do you know your tests can fail?", "Deliberate bugs were put into the code one at a time, such as opening unlisted pages or removing the last-administrator rule, and each time the matching tests failed. The code was then restored."),
        ("Why are tests run on a separate database?", "Many tests insert and delete rows. A separate apparat2_test means they cannot disturb demo data. A guard refuses to run a test against any database whose name does not end in _test."),
        ("Which Java concepts does this part demonstrate?", "Enums with fields and methods (Role), an abstract class with subclasses and polymorphism (BookingPolicy), interfaces (the Filter interface that AuthFilter implements), custom exceptions, records, collections and streams (the home page summary), and threads with locks and transactions in the concurrency tests."),
        ("What would you improve next?", "Per-action permissions, editable limits, CSRF tokens and a self-service password change. Each is a listed limit."),
    ])
    doc.build(s)
    return path

if __name__ == "__main__":
    print(build_nazrin())
    print(build_shourya())
    print(build_roles())
