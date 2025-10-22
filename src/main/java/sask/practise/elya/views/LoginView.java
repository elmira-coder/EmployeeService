package sask.practise.elya.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Вход | Система учета сотрудников")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Настраиваем форму логина
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        // Русские надписи - исправленная версия
        login.setI18n(createRussianLoginI18n());

        // Заголовок и описание
        H1 title = new H1("Система учета сотрудников");
        Paragraph description = new Paragraph("Войдите в систему для доступа к данным");

        VerticalLayout loginLayout = new VerticalLayout();
        loginLayout.setWidth("300px");
        loginLayout.setAlignItems(Alignment.CENTER);
        loginLayout.add(title, description, login);

        add(loginLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Показываем ошибку если была попытка неудачного входа
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }

    private LoginI18n createRussianLoginI18n() {
        LoginI18n i18n = LoginI18n.createDefault();

        // Заголовок формы
        LoginI18n.Header header = new LoginI18n.Header();
        header.setTitle("Система учета сотрудников");
        header.setDescription("Войдите в систему используя admin/admin");
        i18n.setHeader(header);

        // Форма
        LoginI18n.Form form = new LoginI18n.Form();
        form.setTitle("Вход");
        form.setUsername("Имя пользователя");
        form.setPassword("Пароль");
        form.setSubmit("Войти");
        form.setForgotPassword("Забыли пароль?");
        i18n.setForm(form);

        // Сообщение об ошибке
        LoginI18n.ErrorMessage errorMessage = new LoginI18n.ErrorMessage();
        errorMessage.setTitle("Ошибка входа");
        errorMessage.setMessage("Неверное имя пользователя или пароль");
        errorMessage.setUsername("Имя пользователя обязательно");
        errorMessage.setPassword("Пароль обязателен");
        i18n.setErrorMessage(errorMessage);

        return i18n;
    }
}