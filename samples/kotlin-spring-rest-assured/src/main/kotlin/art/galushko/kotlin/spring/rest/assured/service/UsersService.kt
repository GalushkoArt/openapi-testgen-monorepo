package art.galushko.kotlin.spring.rest.assured.service

import art.galushko.kotlin.spring.rest.assured.model.User
import art.galushko.kotlin.spring.rest.assured.model.UserList
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class UsersService {

    private val users: MutableList<User> = mutableListOf()

    init {
        users.add(
            User("u_123", "Ada Lovelace", "ada@example.com", OffsetDateTime.parse("2025-07-01T12:00:00Z"))
        )
        users.add(
            User("u_124", "Alan Turing", "alan@example.com", OffsetDateTime.parse("2025-07-03T12:00:00Z"))
        )
    }

    fun listUsers(page: Int?, pageSize: Int?, q: String?): UserList {
        val safePage = page?.takeIf { it >= 1 } ?: 1
        val safeSize = pageSize?.takeIf { it >= 1 }?.coerceAtMost(100) ?: 20

        val filtered = if (!q.isNullOrBlank()) {
            val needle = q.lowercase()
            users.filter { user ->
                user.name.lowercase().contains(needle) || user.email.lowercase().contains(needle)
            }
        } else {
            users
        }

        val from = minOf((safePage - 1) * safeSize, filtered.size)
        val to = minOf(from + safeSize, filtered.size)
        val pageItems = filtered.subList(from, to)

        return UserList(safePage, safeSize, filtered.size, pageItems)
    }

    fun getUser(userId: String): User? {
        return users.find { it.id == userId }
    }
}
