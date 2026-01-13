package art.galushko.kotlin.spring.rest.assured.api

import art.galushko.kotlin.spring.rest.assured.model.NewUser
import art.galushko.kotlin.spring.rest.assured.model.User
import art.galushko.kotlin.spring.rest.assured.model.UserList
import art.galushko.kotlin.spring.rest.assured.service.UsersService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class UsersApiDelegateImpl(
    private val usersService: UsersService
) : UsersApiDelegate {

    override fun listUsers(
        page: Int?,
        pageSize: Int?,
        q: String?
    ): ResponseEntity<UserList> {
        val result = usersService.listUsers(page, pageSize, q)
        return ResponseEntity.ok(result)
    }

    override fun getUser(userId: String): ResponseEntity<User> {
        val user = usersService.getUser(userId)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    override fun createUser(
        newUser: NewUser,
        idempotencyKey: String?
    ): ResponseEntity<User> {
        val created = User(
            "u_999",
            newUser.name,
            newUser.email,
            OffsetDateTime.now()
        )
        return ResponseEntity.created(java.net.URI.create("/v1/users/" + created.id)).body(created)
    }
}
