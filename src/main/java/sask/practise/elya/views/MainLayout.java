package sask.practise.elya.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("Система учета сотрудников");
        title.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM
        );

        DrawerToggle toggle = new DrawerToggle();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Span userInfo = new Span("Пользователь: " + username);
        userInfo.getStyle()
                .set("margin-right", "1em")
                .set("margin-left", "auto"); // Это вытолкнет элемент вправо

        Button logoutButton = new Button("Выйти", VaadinIcon.SIGN_OUT.create(), e -> {
            getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
        });
        logoutButton.getStyle()
                .set("color", "white")
                .set("background-color", "#f44336");

        // Создаем контейнер для правой части
        Div rightContainer = new Div();
        rightContainer.add(userInfo, logoutButton);
        rightContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("margin-left", "auto"); // Это выровняет контейнер по правому краю

        addToNavbar(toggle, title, rightContainer);
    }

    private void createDrawer() {
        // Создаем вкладки для навигации (убрали дублирование текста)
        Tab dashboardTab = createTab(VaadinIcon.DASHBOARD, "Главная", MainView.class);
        Tab employeesTab = createTab(VaadinIcon.USERS, "Сотрудники", EmployeeView.class);
        Tab departmentsTab = createTab(VaadinIcon.BUILDING, "Отделы", DepartmentView.class);
        Tab positionsTab = createTab(VaadinIcon.BRIEFCASE, "Должности", PositionView.class);
        Tab salariesTab = createTab(VaadinIcon.MONEY, "Зарплаты", SalaryView.class);
        Tab excelTab = createTab(VaadinIcon.FILE_TABLE, "Excel Эскпорт/Импорт", ExcelImportView.class);

        Tabs tabs = new Tabs(dashboardTab, employeesTab, departmentsTab, positionsTab, salariesTab, excelTab);
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames(LumoUtility.Flex.GROW);

        addToDrawer(tabs);
    }

    private Tab createTab(VaadinIcon icon, String label, Class<?> navigationTarget) {
        Icon tabIcon = icon.create();
        tabIcon.getStyle().set("width", "var(--lumo-icon-size-s)");
        tabIcon.getStyle().set("height", "var(--lumo-icon-size-s)");
        tabIcon.getStyle().set("margin-right", "var(--lumo-space-s)");

        // Убрали Span с текстом из RouterLink, оставили только иконку
        RouterLink link = new RouterLink();
        link.add(tabIcon, new Span(label)); // Теперь текст только в Span
        link.setRoute((Class<? extends Component>) navigationTarget);
        link.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);

        Tab tab = new Tab(link);
        tab.addClassName("nav-tab");

        return tab;
    }
}