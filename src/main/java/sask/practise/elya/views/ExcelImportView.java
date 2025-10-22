package sask.practise.elya.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import sask.practise.elya.models.Department;
import sask.practise.elya.models.Employee;
import sask.practise.elya.models.Position;
import sask.practise.elya.models.Salary;
import sask.practise.elya.repositories.DepartmentRepository;
import sask.practise.elya.repositories.EmployeeRepository;
import sask.practise.elya.repositories.PositionRepository;
import sask.practise.elya.repositories.SalaryRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Route(value = "excel-export-import", layout = MainLayout.class)
@PageTitle("Экспорт/Импорт Excel | Система учета сотрудников")
public class ExcelImportView extends VerticalLayout {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final SalaryRepository salaryRepository;

    @Autowired
    public ExcelImportView(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           SalaryRepository salaryRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.salaryRepository = salaryRepository;

        initView();
    }

    private void initView() {
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        H2 title = new H2("Импорт/Экспорт данных Excel");
        Paragraph description = new Paragraph("Выберите действие для работы с Excel файлами");

        // Кнопки для экспорта
        Button exportAllButton = new Button("Экспорт всех данных", e -> showExportAllDialog());
        Button exportSelectiveButton = new Button("Выборочный экспорт", e -> showExportSelectiveDialog());

        exportAllButton.getStyle().set("background-color", "#4CAF50").set("color", "white");
        exportSelectiveButton.getStyle().set("background-color", "#2196F3").set("color", "white");

        HorizontalLayout exportButtons = new HorizontalLayout(exportAllButton, exportSelectiveButton);
        exportButtons.setSpacing(true);

        // Загрузка для импорта
        H2 importTitle = new H2("Импорт данных из Excel");
        Paragraph importDescription = new Paragraph("Загрузите Excel файл для импорта данных");

        FileBuffer buffer = new FileBuffer();
        Upload upload = createUploadComponent(buffer);

        add(title, description, exportButtons, importTitle, importDescription, upload);
    }

    private Upload createUploadComponent(FileBuffer buffer) {
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setMaxFileSize(10 * 1024 * 1024); // 10MB

        // Кастомизация текстов
        UploadI18N i18n = new UploadI18N();
        UploadI18N.Uploading uploading = new UploadI18N.Uploading();
        uploading.setStatus(new UploadI18N.Uploading.Status());
        uploading.getStatus().setConnecting("Подключение...");
        uploading.getStatus().setStalled("Загрузка остановлена");
        uploading.getStatus().setProcessing("Обработка файла...");
        uploading.setRemainingTime(new UploadI18N.Uploading.RemainingTime());
        uploading.getRemainingTime().setPrefix("осталось: ");
        uploading.setError(new UploadI18N.Uploading.Error());
        uploading.getError().setServerUnavailable("Сервер недоступен");
        uploading.getError().setUnexpectedServerError("Неожиданная ошибка сервера");
        uploading.getError().setForbidden("Запрещено");

        i18n.setUploading(uploading);
        upload.setI18n(i18n);

        upload.addFileRejectedListener(event -> {
            Notification.show("Ошибка: " + event.getErrorMessage(), 5000, Notification.Position.MIDDLE);
        });

        upload.addFinishedListener(event -> {
            try {
                handleFileUpload(buffer.getInputStream(), event.getFileName());
                Notification.show("Файл успешно обработан: " + event.getFileName(), 3000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Ошибка при обработке файла: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                ex.printStackTrace();
            }
        });

        return upload;
    }

    private void showExportAllDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Экспорт всех данных");

        Paragraph description = new Paragraph("Будет создан Excel файл со всеми данными системы (сотрудники, отделы, должности, зарплаты)");

        Button confirmButton = new Button("Скачать", e -> {
            exportAllData();
            dialog.close();
        });
        Button cancelButton = new Button("Отмена", e -> dialog.close());

        confirmButton.getStyle().set("background-color", "#4CAF50").set("color", "white");

        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout content = new VerticalLayout(description, buttons);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }

    private void showExportSelectiveDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Выборочный экспорт");

        Paragraph description = new Paragraph("Выберите таблицу для экспорта");

        ComboBox<String> tableComboBox = new ComboBox<>("Таблица");
        tableComboBox.setItems("Сотрудники", "Отделы", "Должности", "Зарплаты");
        tableComboBox.setPlaceholder("Выберите таблицу");

        Button confirmButton = new Button("Скачать", e -> {
            if (tableComboBox.getValue() != null) {
                exportSelectiveData(tableComboBox.getValue());
                dialog.close();
            } else {
                Notification.show("Выберите таблицу для экспорта", 3000, Notification.Position.MIDDLE);
            }
        });
        Button cancelButton = new Button("Отмена", e -> dialog.close());

        confirmButton.setEnabled(false);
        confirmButton.getStyle().set("background-color", "#2196F3").set("color", "white");
        tableComboBox.addValueChangeListener(e -> confirmButton.setEnabled(e.getValue() != null));

        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout content = new VerticalLayout(description, tableComboBox, buttons);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }

    private void exportAllData() {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Лист сотрудников
            createEmployeeSheet(workbook);

            // Лист отделов
            createDepartmentSheet(workbook);

            // Лист должностей
            createPositionSheet(workbook);

            // Лист зарплат
            createSalarySheet(workbook);

            // Создание ресурса для скачивания
            String fileName = "Полный_экспорт_системы_сотрудников.xlsx";
            StreamResource resource = createStreamResource(workbook, fileName);

            // Создание и клик по ссылке для скачивания
            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);

            downloadExcelFile(workbook, fileName);

            Notification.show("Файл успешно создан!", 3000, Notification.Position.MIDDLE);

        } catch (Exception e) {
            Notification.show("Ошибка при создании файла: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            e.printStackTrace();
        }
    }

    private void exportSelectiveData(String tableName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            switch (tableName) {
                case "Сотрудники":
                    createEmployeeSheet(workbook);
                    break;
                case "Отделы":
                    createDepartmentSheet(workbook);
                    break;
                case "Должности":
                    createPositionSheet(workbook);
                    break;
                case "Зарплаты":
                    createSalarySheet(workbook);
                    break;
            }

            String fileName = tableName.toLowerCase() + "_export.xlsx";
            StreamResource resource = createStreamResource(workbook, fileName);

            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);

            downloadExcelFile(workbook, fileName);

            Notification.show("Файл " + tableName + " успешно создан!", 3000, Notification.Position.MIDDLE);

        } catch (Exception e) {
            Notification.show("Ошибка при создании файла: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            e.printStackTrace();
        }
    }

    private StreamResource createStreamResource(Workbook workbook, String fileName) {
        return new StreamResource(fileName, () -> {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Ошибка создания файла", e);
            }
        });
    }

    private void createEmployeeSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Сотрудники");

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "ФИО", "Отдел", "Должность", "Дата приёма"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Данные
        List<Employee> employees = employeeRepository.findAllWithDetails();
        int rowNum = 1;
        for (Employee employee : employees) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(employee.getId() != null ? employee.getId() : 0);
            row.createCell(1).setCellValue(employee.getFullName() != null ? employee.getFullName() : "");
            row.createCell(2).setCellValue(employee.getDepartment() != null ? employee.getDepartment().getName() : "");
            row.createCell(3).setCellValue(employee.getPosition() != null ? employee.getPosition().getName() : "");
            row.createCell(4).setCellValue(employee.getAdmissionDate() != null ? employee.getAdmissionDate().toString() : "");
        }

        // Авто-размер колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createDepartmentSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Отделы");

        String[] headers = {"ID", "Название", "Описание"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Department> departments = departmentRepository.findAll();
        int rowNum = 1;
        for (Department department : departments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(department.getId() != null ? department.getId() : 0);
            row.createCell(1).setCellValue(department.getName() != null ? department.getName() : "");
            row.createCell(2).setCellValue(department.getDescription() != null ? department.getDescription() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createPositionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Должности");

        String[] headers = {"ID", "Название", "Описание"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Position> positions = positionRepository.findAll();
        int rowNum = 1;
        for (Position position : positions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(position.getId() != null ? position.getId() : 0);
            row.createCell(1).setCellValue(position.getName() != null ? position.getName() : "");
            row.createCell(2).setCellValue(position.getDescription() != null ? position.getDescription() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSalarySheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Зарплаты");

        String[] headers = {"ID", "Сотрудник", "Зарплата", "Дата начала"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Salary> salaries = salaryRepository.findAll();
        int rowNum = 1;
        for (Salary salary : salaries) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(salary.getId() != null ? salary.getId() : 0);
            row.createCell(1).setCellValue(salary.getEmployee() != null ? salary.getEmployee().getFullName() : "");
            row.createCell(2).setCellValue(salary.getAmount() != null ? salary.getAmount() : 0);
            row.createCell(3).setCellValue(salary.getStartDate() != null ? salary.getStartDate().toString() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void handleFileUpload(InputStream inputStream, String fileName) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            int importedCount = 0;

            // Импорт отделов
            Sheet deptSheet = workbook.getSheet("Отделы");
            if (deptSheet != null) {
                importedCount += importDepartments(deptSheet);
            }

            // Импорт должностей
            Sheet positionSheet = workbook.getSheet("Должности");
            if (positionSheet != null) {
                importedCount += importPositions(positionSheet);
            }

            // Импорт сотрудников
            Sheet employeeSheet = workbook.getSheet("Сотрудники");
            if (employeeSheet != null) {
                importedCount += importEmployees(employeeSheet);
            }

            // Импорт зарплат
            Sheet salarySheet = workbook.getSheet("Зарплаты");
            if (salarySheet != null) {
                importedCount += importSalaries(salarySheet);
            }

            Notification.show("Импорт завершен. Обработано записей: " + importedCount, 5000, Notification.Position.MIDDLE);

        } catch (Exception e) {
            Notification.show("Ошибка при импорте файла: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            e.printStackTrace();
        }
    }

    private int importDepartments(Sheet sheet) {
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && hasData(row)) {
                try {
                    Department department = new Department();
                    department.setName(getStringCellValue(row.getCell(1)));
                    department.setDescription(getStringCellValue(row.getCell(2)));

                    // Проверяем, существует ли уже отдел с таким именем
                    if (department.getName() != null && !department.getName().trim().isEmpty()) {
                        if (departmentRepository.findByName(department.getName()).isEmpty()) {
                            departmentRepository.save(department);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка импорта отдела в строке " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private int importPositions(Sheet sheet) {
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && hasData(row)) {
                try {
                    Position position = new Position();
                    position.setName(getStringCellValue(row.getCell(1)));
                    position.setDescription(getStringCellValue(row.getCell(2)));

                    if (position.getName() != null && !position.getName().trim().isEmpty()) {
                        if (positionRepository.findByName(position.getName()).isEmpty()) {
                            positionRepository.save(position);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка импорта должности в строке " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private int importEmployees(Sheet sheet) {
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && hasData(row)) {
                try {
                    Employee employee = new Employee();
                    employee.setFullName(getStringCellValue(row.getCell(1)));

                    // Поиск отдела по имени
                    String deptName = getStringCellValue(row.getCell(2));
                    if (deptName != null && !deptName.trim().isEmpty()) {
                        departmentRepository.findByName(deptName).ifPresent(employee::setDepartment);
                    }

                    // Поиск должности по имени
                    String positionName = getStringCellValue(row.getCell(3));
                    if (positionName != null && !positionName.trim().isEmpty()) {
                        positionRepository.findByName(positionName).ifPresent(employee::setPosition);
                    }

                    // Дата приёма
                    String dateStr = getStringCellValue(row.getCell(4));
                    if (dateStr != null && !dateStr.trim().isEmpty()) {
                        employee.setAdmissionDate(LocalDate.parse(dateStr));
                    } else {
                        employee.setAdmissionDate(LocalDate.now());
                    }

                    if (employee.getFullName() != null && !employee.getFullName().trim().isEmpty()) {
                        employeeRepository.save(employee);
                        count++;
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка импорта сотрудника в строке " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private int importSalaries(Sheet sheet) {
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && hasData(row)) {
                try {
                    Salary salary = new Salary();

                    // Поиск сотрудника по имени
                    String employeeName = getStringCellValue(row.getCell(1));
                    if (employeeName != null && !employeeName.trim().isEmpty()) {
                        employeeRepository.findByFullName(employeeName).ifPresent(salary::setEmployee);
                    }

                    // Зарплата
                    Cell amountCell = row.getCell(2);
                    if (amountCell != null && amountCell.getCellType() == CellType.NUMERIC) {
                        salary.setAmount((long) amountCell.getNumericCellValue());
                    } else if (amountCell != null && amountCell.getCellType() == CellType.STRING) {
                        try {
                            salary.setAmount(Long.parseLong(amountCell.getStringCellValue().trim()));
                        } catch (NumberFormatException e) {
                            // Пропускаем некорректные значения
                        }
                    }

                    // Дата начала
                    String dateStr = getStringCellValue(row.getCell(3));
                    if (dateStr != null && !dateStr.trim().isEmpty()) {
                        salary.setStartDate(LocalDate.parse(dateStr));
                    } else {
                        salary.setStartDate(LocalDate.now());
                    }

                    if (salary.getEmployee() != null && salary.getAmount() != null) {
                        salaryRepository.save(salary);
                        count++;
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка импорта зарплаты в строке " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private boolean hasData(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getStringCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate().toString();
                } else {
                    // Для числовых значений убираем дробную часть если она .0
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    } else {
                        return String.valueOf(num);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    private void downloadExcelFile(Workbook workbook, String fileName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();

            // Конвертируем байты в base64
            String base64Data = java.util.Base64.getEncoder().encodeToString(bytes);

            // JavaScript для создания и скачивания файла
            String js = String.format(
                    "const link = document.createElement('a');" +
                            "link.href = 'data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,%s';" +
                            "link.download = '%s';" +
                            "document.body.appendChild(link);" +
                            "link.click();" +
                            "document.body.removeChild(link);",
                    base64Data, fileName
            );

            UI.getCurrent().getPage().executeJs(js);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка создания файла для скачивания", e);
        }
    }
}