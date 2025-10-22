package sask.practise.elya.config;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import sask.practise.elya.views.LoginView;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@Configuration
@EnableWebSecurity
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .with(vaadin(), vaadin -> {
                    vaadin.loginView(LoginView.class, "/login?logout");
                    vaadin.enableCsrfConfiguration(false);
                })
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/public/**").permitAll())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Для примера — один пользователь admin
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
