package art.galushko.kotlin.spring.rest.assured.api

import art.galushko.kotlin.spring.rest.assured.model.NewOrder
import art.galushko.kotlin.spring.rest.assured.model.Order
import art.galushko.kotlin.spring.rest.assured.model.OrderList
import art.galushko.kotlin.spring.rest.assured.model.Status
import art.galushko.kotlin.spring.rest.assured.service.OrdersService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class OrdersApiDelegateImpl(
    private val ordersService: OrdersService
) : OrdersApiDelegate {

    override fun listOrders(
        page: Int?,
        pageSize: Int?,
        status: Status?,
        userId: String?,
    ): ResponseEntity<OrderList> {
        return ResponseEntity.ok(ordersService.listOrders(page, pageSize, userId, status))
    }

    override fun createOrder(
        newOrder: NewOrder,
        idempotencyKey: String?,
    ): ResponseEntity<Order> {
        val created = Order(
            "o_999",
            newOrder.userId,
            Status.PENDING,
            BigDecimal.ZERO, // Would calculate in real implementation
            "USD",
            OffsetDateTime.now(),
            newOrder.items
        )
        return ResponseEntity.created(java.net.URI.create("/v1/orders/" + created.id)).body(created)
    }
}
