package sask.practise.elya.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import sask.practise.elya.models.Department;
import sask.practise.elya.models.Employee;
import sask.practise.elya.models.Position;
import sask.practise.elya.repositories.DepartmentRepository;
import sask.practise.elya.repositories.EmployeeRepository;
import sask.practise.elya.repositories.PositionRepository;

@Route(value = "employees", layout = MainLayout.class)
@PageTitle("Сотрудники | Система учета сотрудников")
public class EmployeeView extends VerticalLayout {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    private Grid<Employee> grid = new Grid<>(Employee.class, false);
    private TextField fullName = new TextField("ФИО");
    private ComboBox<Department> department = new ComboBox<>("Отдел");
    private ComboBox<Position> position = new ComboBox<>("Должность");
    private DatePicker admissionDate = new DatePicker("Дата приёма");
    private Button saveButton = new Button("Сохранить");
    private Button cancelButton = new Button("Отмена");
    private Button deleteButton = new Button("Удалить");

    private Binder<Employee> binder = new Binder<>(Employee.class);
    private Employee currentEmployee;

    @Autowired
    public EmployeeView(EmployeeRepository employeeRepository,
                        DepartmentRepository departmentRepository,
                        PositionRepository positionRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
    }

    @PostConstruct
    public void init() {
        configureGrid();
        configureForm();

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, cancelButton);
        HorizontalLayout formLayout = new HorizontalLayout(fullName, department, position, admissionDate);

        add(grid, formLayout, buttonLayout);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Employee::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Employee::getFullName).setHeader("ФИО").setAutoWidth(true);
        grid.addColumn(e -> e.getDepartment() != null ? e.getDepartment().getName() : "").setHeader("Отдел").setAutoWidth(true);
        grid.addColumn(e -> e.getPosition() != null ? e.getPosition().getName() : "").setHeader("Должность").setAutoWidth(true);
        grid.addColumn(Employee::getAdmissionDate).setHeader("Дата приёма").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editEmployee(event.getValue());
            } else {
                clearForm();
            }
        });
    }

    private void configureForm() {
        department.setItems(departmentRepository.findAll());
        department.setItemLabelGenerator(Department::getName);

        position.setItems(positionRepository.findAll());
        position.setItemLabelGenerator(Position::getName);

        binder.forField(fullName).asRequired("ФИО обязательно").bind(Employee::getFullName, Employee::setFullName);
        binder.forField(department).asRequired("Отдел обязателен").bind(Employee::getDepartment, Employee::setDepartment);
        binder.forField(position).asRequired("Должность обязательна").bind(Employee::getPosition, Employee::setPosition);
        binder.forField(admissionDate).asRequired("Дата приёма обязательна").bind(Employee::getAdmissionDate, Employee::setAdmissionDate);

        saveButton.addClickListener(e -> saveEmployee());
        deleteButton.addClickListener(e -> deleteEmployee());
        cancelButton.addClickListener(e -> clearForm());

        // Начальное состояние формы
        clearForm();
    }

    private void editEmployee(Employee employee) {
        currentEmployee = employee;
        binder.setBean(employee);
        deleteButton.setEnabled(true);
    }

    private void clearForm() {
        currentEmployee = null;
        binder.setBean(new Employee());
        deleteButton.setEnabled(false);
        grid.deselectAll();
    }

    private void saveEmployee() {
        if (binder.validate().isOk()) {
            Employee employeeToSave = binder.getBean();
            employeeRepository.save(employeeToSave);
            refreshGrid();
            clearForm();
        }
    }

    private void deleteEmployee() {
        if (currentEmployee != null && currentEmployee.getId() != null) {
            employeeRepository.delete(currentEmployee);
            refreshGrid();
            clearForm();
        }
    }

    @Transactional(readOnly = true)
    public void refreshGrid() {
        grid.setItems(employeeRepository.findAll());
    }
}