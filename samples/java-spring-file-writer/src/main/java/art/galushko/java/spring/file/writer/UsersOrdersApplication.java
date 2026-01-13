package art.galushko.java.spring.file.writer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;

@SpringBootApplication
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
public class UsersOrdersApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsersOrdersApplication.class, args);
    }
}


