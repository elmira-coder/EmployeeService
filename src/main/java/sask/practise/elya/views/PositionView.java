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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import sask.practise.elya.models.Position;
import sask.practise.elya.repositories.PositionRepository;

@Route(value = "positions", layout = MainLayout.class)
@PageTitle("Должности | Система учета сотрудников")
public class PositionView extends VerticalLayout {

    private final PositionRepository positionRepository;

    private Grid<Position> grid = new Grid<>(Position.class, false);
    private TextField nameField = new TextField("Название должности");
    private TextArea descriptionField = new TextArea("Описание"); // Изменено на TextArea
    private Button saveButton = new Button("Сохранить");
    private Button deleteButton = new Button("Удалить");
    private Button cancelButton = new Button("Отмена");

    private Binder<Position> binder = new Binder<>(Position.class);

    @Autowired
    public PositionView(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
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
        grid.addColumn(Position::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Position::getName).setHeader("Название").setAutoWidth(true);
        grid.addColumn(Position::getDescription).setHeader("Описание").setAutoWidth(true);

        grid.addColumn(pos -> positionRepository.countEmployeesByPositionId(pos.getId()))
                .setHeader("Кол-во сотрудников").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editPosition(event.getValue());
            } else {
                clearForm();
            }
        });
    }

    private void configureForm() {
        binder.forField(nameField)
                .asRequired("Название обязательно")
                .bind(Position::getName, Position::setName);

        binder.forField(descriptionField)
                .asRequired("Описание обязательно")
                .bind(Position::getDescription, Position::setDescription);

        saveButton.addClickListener(e -> savePosition());
        deleteButton.addClickListener(e -> deletePosition());
        cancelButton.addClickListener(e -> clearForm());

        clearForm();
    }

    private void editPosition(Position position) {
        // Создаем копию для редактирования
        Position positionCopy = new Position();
        positionCopy.setId(position.getId());
        positionCopy.setName(position.getName());
        positionCopy.setDescription(position.getDescription());

        binder.setBean(positionCopy);
        deleteButton.setEnabled(true);
    }

    private void clearForm() {
        // Создаем полностью новый объект
        Position newPosition = new Position();
        newPosition.setName("");
        newPosition.setDescription("");
        binder.setBean(newPosition);
        deleteButton.setEnabled(false);
        grid.deselectAll();
    }

    private void savePosition() {
        if (binder.validate().isOk()) {
            try {
                Position positionToSave = binder.getBean();

                // Всегда сохраняем как новую сущность, если ID не установлен
                Position savedPosition = positionRepository.save(positionToSave);

                refreshGrid();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
                // В случае ошибки очищаем форму
                clearForm();
            }
        }
    }

    private void deletePosition() {
        Position currentPosition = binder.getBean();
        if (currentPosition != null && currentPosition.getId() != null) {
            try {
                long employeeCount = positionRepository.countEmployeesByPositionId(currentPosition.getId());
                if (employeeCount > 0) {
                    // Можно добавить уведомление
                    return;
                }
                positionRepository.deleteById(currentPosition.getId());
                refreshGrid();
                clearForm();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional(readOnly = true)
    public void refreshGrid() {
        grid.setItems(positionRepository.findAll());
    }
}