package app.audit;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

  static final String USER_HEADER = "X-User";
  static final String NO_USER = "system";

  @Bean
  public AuditorAware<String> auditor() {
    return () ->
        Optional.of(
            Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(a -> ((ServletRequestAttributes) a).getRequest().getHeader(USER_HEADER))
                .filter(user -> !user.isBlank())
                .orElse(NO_USER));
  }
}
