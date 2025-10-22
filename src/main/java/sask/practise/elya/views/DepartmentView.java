package sask.practise.elya.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import sask.practise.elya.models.Department;
import sask.practise.elya.repositories.DepartmentRepository;

@PermitAll
@Route(value = "departments", layout = MainLayout.class)
@PageTitle("Отделы | Система учета сотрудников")
public class DepartmentView extends VerticalLayout {

    private final DepartmentRepository departmentRepository;

    private Grid<Department> grid = new Grid<>(Department.class, false);
    private TextField nameField = new TextField("Название отдела");
    private TextArea descriptionField = new TextArea("Описание"); // Изменено на TextArea
    private Button saveButton = new Button("Сохранить");
    private Button deleteButton = new Button("Удалить");
    private Button cancelButton = new Button("Отмена");

    private Binder<Department> binder = new Binder<>(Department.class);

    @Autowired
    public DepartmentView(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @PostConstruct
    public void init() {
        configureGrid();
        configureForm();

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, deleteButton, cancelButton);

        // Вертикальная компоновка для формы с большим полем описания
        VerticalLayout formLayout = new VerticalLayout(nameField, descriptionField, buttonLayout);
        formLayout.setWidthFull();
        descriptionField.setWidthFull();
        descriptionField.setHeight("150px"); // Устанавливаем высоту

        add(grid, formLayout);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Department::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Department::getName).setHeader("Название").setAutoWidth(true);
        grid.addColumn(Department::getDescription).setHeader("Описание").setAutoWidth(true);

        grid.addColumn(dept -> departmentRepository.countEmployeesByDepartmentId(dept.getId()))
                .setHeader("Кол-во сотрудников").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editDepartment(event.getValue());
            } else {
                clearForm();
            }
        });
    }

    private void configureForm() {
        binder.forField(nameField)
                .asRequired("Название обязательно")
                .bind(Department::getName, Department::setName);

        binder.forField(descriptionField)
                .asRequired("Описание обязательно")
                .bind(Department::getDescription, Department::setDescription);

        saveButton.addClickListener(e -> saveDepartment());
        deleteButton.addClickListener(e -> deleteDepartment());
        cancelButton.addClickListener(e -> clearForm());

        clearForm();
    }

    private void editDepartment(Department department) {
        // Создаем копию для редактирования
        Department departmentCopy = new Department();
        departmentCopy.setId(department.getId());
        departmentCopy.setName(department.getName());
        departmentCopy.setDescription(department.getDescription());

        binder.setBean(departmentCopy);
        deleteButton.setEnabled(true);
    }

    private void clearForm() {
        // Создаем полностью новый объект
        Department newDepartment = new Department();
        newDepartment.setName("");
        newDepartment.setDescription("");
        binder.setBean(newDepartment);
        deleteButton.setEnabled(false);
        grid.deselectAll();
    }

    private void saveDepartment() {
        if (binder.validate().isOk()) {
            try {
                Department departmentToSave = binder.getBean();

                // Всегда сохраняем как новую сущность, если ID не установлен
                Department savedDepartment = departmentRepository.save(departmentToSave);

                refreshGrid();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
                // В случае ошибки очищаем форму
                clearForm();
            }
        }
    }

    private void deleteDepartment() {
        Department currentDepartment = binder.getBean();
        if (currentDepartment != null && currentDepartment.getId() != null) {
            try {
                long employeeCount = departmentRepository.countEmployeesByDepartmentId(currentDepartment.getId());
                if (employeeCount > 0) {
                    // Можно добавить уведомление
                    return;
                }
                departmentRepository.deleteById(currentDepartment.getId());
                refreshGrid();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional(readOnly = true)
    public void refreshGrid() {
        grid.setItems(departmentRepository.findAll());
    }
}