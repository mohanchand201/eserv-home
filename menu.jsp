<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Navigation Menu</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/styles.css">
    <script src="/js/scripts.js"></script>
</head>
<body>
    <div class="container">
        <aside>
            <div class="toggle">
                <div class="logo">
                    
                </div>
                <div class="close" id="close-btn">
                    <span class="material-icons-sharp">
                        close
                    </span>
                </div>
            </div>
            <div class="sidebar">
            <c:forEach var="menuItem" items="${menuItems}">
                        <a href="<c:out value='${menuItem.url}' />" target="body" id="<c:out value='${menuItem.id}' />">
                            <span class="material-icons-sharp"><c:out value="${menuItem.iconName}" /></span>
                            <h3><c:out value="${menuItem.label}" /></h3>
                        </a>
            </c:forEach>
            </div>
        </aside>
    </div>
    <script>
        const sideMenu = document.querySelector('aside');
const menuBtn = document.getElementById('menu-btn');
const closeBtn = document.getElementById('close-btn');

const darkMode = document.querySelector('.dark-mode');

menuBtn.addEventListener('click', () => {
    sideMenu.style.display = 'block';
});

closeBtn.addEventListener('click', () => {
    sideMenu.style.display = 'none';
});

darkMode.addEventListener('click', () => {
    document.body.classList.toggle('dark-mode-variables');
    darkMode.querySelector('span:nth-child(1)').classList.toggle('active');
    darkMode.querySelector('span:nth-child(2)').classList.toggle('active');
})
    </script>
</body>
</html>
