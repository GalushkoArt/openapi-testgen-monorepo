package art.galushko.kotlin.spring.rest.assured.service

import art.galushko.kotlin.spring.rest.assured.model.Order
import art.galushko.kotlin.spring.rest.assured.model.OrderItem
import art.galushko.kotlin.spring.rest.assured.model.OrderList
import art.galushko.kotlin.spring.rest.assured.model.Status
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class OrdersService {

    private val orders: MutableList<Order> = mutableListOf()

    init {
        val items1 = listOf(
            OrderItem("SKU-1", 1, BigDecimal("99.95")),
            OrderItem("SKU-2", 1, BigDecimal("50.00"))
        )
        orders.add(
            Order(
                "o_1000",
                "u_123",
                Status.PAID,
                BigDecimal("149.95"),
                "USD",
                OffsetDateTime.parse("2025-08-10T09:30:00Z"),
                items1
            )
        )
    }

    fun listOrders(page: Int?, pageSize: Int?, userId: String?, status: Status?): OrderList {
        val safePage = page?.takeIf { it >= 1 } ?: 1
        val safeSize = pageSize?.takeIf { it >= 1 }?.coerceAtMost(100) ?: 20

        val filtered = orders.filter { order ->
            val userIdMatch = userId == null || order.userId == userId
            val statusMatch = status == null || order.status == status
            userIdMatch && statusMatch
        }

        val from = minOf((safePage - 1) * safeSize, filtered.size)
        val to = minOf(from + safeSize, filtered.size)
        val pageItems = filtered.subList(from, to)

        return OrderList(safePage, safeSize, filtered.size, pageItems)
    }
}
