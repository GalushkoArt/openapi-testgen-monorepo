package art.galushko.kotlin.spring.rest.assured

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration

@SpringBootApplication
@ImportAutoConfiguration(ValidationAutoConfiguration::class)
open class UsersOrdersApplication

fun main(args: Array<String>) {
    SpringApplication.run(UsersOrdersApplication::class.java, *args)
}
