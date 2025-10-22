package sask.practise.elya.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import sask.practise.elya.repositories.DepartmentRepository;
import sask.practise.elya.repositories.EmployeeRepository;
import sask.practise.elya.repositories.PositionRepository;
import sask.practise.elya.repositories.SalaryRepository;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Главная | Система учета сотрудников")
public class MainView extends VerticalLayout {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final SalaryRepository salaryRepository;

    @Autowired
    public MainView(EmployeeRepository employeeRepository,
                    DepartmentRepository departmentRepository,
                    PositionRepository positionRepository,
                    SalaryRepository salaryRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.salaryRepository = salaryRepository;
    }

    @PostConstruct
    public void init() {
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        H1 welcomeTitle = new H1("Добро пожаловать в систему управления сотрудниками");
        welcomeTitle.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-primary-text-color)");

        Paragraph description = new Paragraph(
                "Используйте боковую панель для навигации по разделам системы."
        );
        description.getStyle().set("text-align", "center");

        // Основная статистика
        H2 basicStatsTitle = new H2("Основная статистика");
        basicStatsTitle.getStyle()
                .set("margin-top", "2em")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "0.5em");

        long employeeCount = employeeRepository.count();
        long departmentCount = departmentRepository.count();
        long positionCount = positionRepository.count();
        long salaryCount = salaryRepository.count();

        HorizontalLayout basicStatsGrid = new HorizontalLayout();
        basicStatsGrid.add(
                createStatCard("👥 Сотрудников", String.valueOf(employeeCount), "#4CAF50"),
                createStatCard("🏢 Отделов", String.valueOf(departmentCount), "#2196F3"),
                createStatCard("💼 Должностей", String.valueOf(positionCount), "#FF9800"),
                createStatCard("💰 Записей о зарплатах", String.valueOf(salaryCount), "#9C27B0")
        );
        basicStatsGrid.setWidthFull();
        basicStatsGrid.setJustifyContentMode(JustifyContentMode.BETWEEN);
        basicStatsGrid.setSpacing(true);

        // Статистика по зарплатам
        H2 salaryStatsTitle = new H2("Статистика по зарплатам");
        salaryStatsTitle.getStyle()
                .set("margin-top", "2em")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("padding-bottom", "0.5em");

        // Получаем статистику по зарплатам
        Double avgSalary = salaryRepository.findAverageSalary();
        Long aboveAvgCount = salaryRepository.countEmployeesWithSalaryAboveAverage();
        Double maxSalary = salaryRepository.findMaxSalary();
        Double minSalary = salaryRepository.findMinSalary();

        HorizontalLayout salaryStatsGrid = new HorizontalLayout();
        salaryStatsGrid.add(
                createStatCard("📊 Средняя зарплата", formatSalary(avgSalary), "#4CAF50"),
                createStatCard("🚀 Выше среднего", String.valueOf(aboveAvgCount != null ? aboveAvgCount : 0), "#FF5722"),
                createStatCard("📈 Макс. зарплата", formatSalary(maxSalary), "#2196F3"),
                createStatCard("📉 Мин. зарплата", formatSalary(minSalary), "#FF9800")
        );
        salaryStatsGrid.setWidthFull();
        salaryStatsGrid.setJustifyContentMode(JustifyContentMode.BETWEEN);
        salaryStatsGrid.setSpacing(true);

        add(welcomeTitle, description, basicStatsTitle, basicStatsGrid, salaryStatsTitle, salaryStatsGrid);

        List<Object[]> deptStats = salaryRepository.findAverageSalaryByDepartment();
        if (deptStats != null && !deptStats.isEmpty()) {
            H2 deptStatsTitle = new H2("Средняя зарплата по отделам");
            deptStatsTitle.getStyle()
                    .set("margin-top", "2em")
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("padding-bottom", "0.5em");

            VerticalLayout deptStatsLayout = new VerticalLayout();
            deptStatsLayout.setSpacing(true);

            for (Object[] stat : deptStats) {
                String deptName = (String) stat[0];
                Double avgSalari = (Double) stat[1];

                HorizontalLayout deptStat = new HorizontalLayout();
                deptStat.setWidthFull();
                deptStat.setJustifyContentMode(JustifyContentMode.BETWEEN);
                deptStat.add(
                        new Paragraph(deptName != null ? deptName : "Без отдела"),
                        new Paragraph(formatSalary(avgSalari))
                );
                deptStatsLayout.add(deptStat);
            }

            add(deptStatsTitle, deptStatsLayout);
        }
    }

    private String formatSalary(Double salary) {
        if (salary == null) return "0 ₽";

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(salary) + " ₽";
    }

    private VerticalLayout createStatCard(String title, String value, String color) {
        H1 valueText = new H1(value);
        valueText.getStyle()
                .set("margin", "0")
                .set("color", color)
                .set("font-size", "var(--lumo-font-size-xxl)");

        Paragraph titleText = new Paragraph(title);
        titleText.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        VerticalLayout card = new VerticalLayout(valueText, titleText);
        card.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("text-align", "center")
                .set("border-left", "4px solid " + color)
                .set("min-width", "200px");
        card.setSpacing(false);
        card.setPadding(true);

        return card;
    }
}