package art.galushko.java.spring.rest.assured.api;

import art.galushko.java.spring.rest.assured.model.NewOrder;
import art.galushko.java.spring.rest.assured.model.Order;
import art.galushko.java.spring.rest.assured.model.OrderList;
import art.galushko.java.spring.rest.assured.model.Status;
import art.galushko.java.spring.rest.assured.service.OrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class OrdersApiDelegateImpl implements OrdersApiDelegate {

	private final OrdersService ordersService;

	public OrdersApiDelegateImpl(OrdersService ordersService) {
		this.ordersService = ordersService;
	}

	@Override
	public ResponseEntity<OrderList> listOrders(Integer page, Integer pageSize, Status status, String userId) {
		return ResponseEntity.ok(ordersService.listOrders(page, pageSize, userId, status));
	}

	@Override
	public ResponseEntity<Order> createOrder(NewOrder newOrder, String idempotencyKey) {
		Order created = new Order()
				.id("o_999")
				.userId(newOrder.getUserId())
				.currency("USD")
				.status(Status.PENDING)
				.items(newOrder.getItems());
		return ResponseEntity.created(java.net.URI.create("/v1/orders/" + created.getId())).body(created);
	}
}


