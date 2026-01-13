package art.galushko.java.spring.rest.assured.service;

import art.galushko.java.spring.rest.assured.model.Order;
import art.galushko.java.spring.rest.assured.model.OrderItem;
import art.galushko.java.spring.rest.assured.model.OrderList;
import art.galushko.java.spring.rest.assured.model.Status;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;

@Service
public class OrdersService {

    private final List<Order> orders;

    public OrdersService() {
        this.orders = new ArrayList<>();

        List<OrderItem> items1 = new ArrayList<>();
        items1.add(new OrderItem().sku("SKU-1").quantity(1).price(BigDecimal.valueOf(99.95)));
        items1.add(new OrderItem().sku("SKU-2").quantity(1).price(BigDecimal.valueOf(50.00)));
        orders.add(new Order()
                .id("o_1000")
                .userId("u_123")
                .status(Status.PAID)
                .total(BigDecimal.valueOf(149.95))
                .currency("USD")
                .createdAt(OffsetDateTime.parse("2025-08-10T09:30:00Z"))
                .items(items1)
        );
    }

    public OrderList listOrders(Integer page, Integer pageSize, String userId, Status status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        List<Order> filtered = new ArrayList<>();
        for (Order o : orders) {
            boolean ok = userId == null || Objects.equals(o.getUserId(), userId);
            if (status != null && !Objects.equals(o.getStatus(), status)) ok = false;
            if (ok) filtered.add(o);
        }

        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<Order> pageItems = filtered.subList(from, to);

        OrderList result = new OrderList();
        result.setPage(safePage);
        result.setPageSize(safeSize);
        result.setTotal(filtered.size());
        result.setItems(pageItems);
        return result;
    }
}


