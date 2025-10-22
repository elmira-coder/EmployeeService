package sask.practise.elya.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import sask.practise.elya.models.Employee;
import sask.practise.elya.models.Salary;
import sask.practise.elya.repositories.EmployeeRepository;
import sask.practise.elya.repositories.SalaryRepository;

import java.time.LocalDate;

@Route(value = "salaries", layout = MainLayout.class)
@PageTitle("Зарплаты | Система учета сотрудников")
public class SalaryView extends VerticalLayout {

    private final SalaryRepository salaryRepository;
    private final EmployeeRepository employeeRepository;

    private Grid<Salary> grid = new Grid<>(Salary.class, false);
    private ComboBox<Employee> employeeComboBox = new ComboBox<>("Сотрудник");
    private NumberField amountField = new NumberField("Размер зарплаты");
    private DatePicker startDateField = new DatePicker("Дата начала");
    private Button saveButton = new Button("Сохранить");
    private Button deleteButton = new Button("Удалить");
    private Button cancelButton = new Button("Отмена");

    private Long currentSalaryId;

    @Autowired
    public SalaryView(SalaryRepository salaryRepository, EmployeeRepository employeeRepository) {
        this.salaryRepository = salaryRepository;
        this.employeeRepository = employeeRepository;
    }

    @PostConstruct
    public void init() {
        configureGrid();
        configureForm();

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, cancelButton);
        HorizontalLayout formLayout = new HorizontalLayout(employeeComboBox, amountField, startDateField);

        add(grid, formLayout, buttonLayout);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Salary::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(s -> s.getEmployee() != null ? s.getEmployee().getFullName() : "Не указан")
                .setHeader("Сотрудник").setAutoWidth(true);
        grid.addColumn(Salary::getAmount).setHeader("Зарплата").setAutoWidth(true);
        grid.addColumn(Salary::getStartDate).setHeader("Дата начала").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editSalary(event.getValue());
            } else {
                clearForm();
            }
        });
    }

    private void configureForm() {
        employeeComboBox.setItems(employeeRepository.findAll());
        employeeComboBox.setItemLabelGenerator(Employee::getFullName);

        amountField.setMin(0);
        amountField.setStep(1000);
        amountField.setValue(0.0);

        startDateField.setValue(LocalDate.now());

        saveButton.addClickListener(e -> saveSalary());
        deleteButton.addClickListener(e -> deleteSalary());
        cancelButton.addClickListener(e -> clearForm());

        clearForm();
    }

    private void editSalary(Salary salary) {
        currentSalaryId = salary.getId();
        employeeComboBox.setValue(salary.getEmployee());
        amountField.setValue(salary.getAmount().doubleValue());
        startDateField.setValue(salary.getStartDate());
        deleteButton.setEnabled(true);
    }

    private void clearForm() {
        currentSalaryId = null;
        employeeComboBox.clear();
        amountField.setValue(0.0);
        startDateField.setValue(LocalDate.now());
        deleteButton.setEnabled(false);
        grid.deselectAll();
    }

    private void saveSalary() {
        // Валидация
        if (employeeComboBox.getValue() == null) {
            return;
        }
        if (amountField.getValue() == null || amountField.getValue() <= 0) {
            return;
        }
        if (startDateField.getValue() == null) {
            return;
        }

        try {
            Salary salary;

            if (currentSalaryId != null) {
                // Обновление существующей записи - загружаем из базы
                salary = salaryRepository.findById(currentSalaryId).orElse(new Salary());
            } else {
                // Создание новой записи
                salary = new Salary();
            }

            // Устанавливаем значения
            salary.setEmployee(employeeComboBox.getValue());
            salary.setAmount(amountField.getValue().longValue());
            salary.setStartDate(startDateField.getValue());

            // Сохраняем
            salaryRepository.save(salary);
            refreshGrid();
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSalary() {
        if (currentSalaryId != null) {
            try {
                // Используем deleteById вместо delete для избежания проблем с persistence context
                salaryRepository.deleteById(currentSalaryId);
                refreshGrid();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional(readOnly = true)
    public void refreshGrid() {
        grid.setItems(salaryRepository.findAll());
    }
}