package com.xiaoyao.examination.order.service;

import com.xiaoyao.examination.common.exception.ErrorCode;
import com.xiaoyao.examination.common.exception.ExaminationException;
import com.xiaoyao.examination.common.interfaces.goods.DiscountService;
import com.xiaoyao.examination.common.interfaces.goods.GoodsService;
import com.xiaoyao.examination.common.interfaces.goods.response.SubmitOrderGoodsInfoResponse;
import com.xiaoyao.examination.common.interfaces.order.OrderService;
import com.xiaoyao.examination.common.interfaces.order.response.UserOrderSummaryResponse;
import com.xiaoyao.examination.common.interfaces.payment.PayService;
import com.xiaoyao.examination.common.interfaces.payment.request.CreatePayOrderRequest;
import com.xiaoyao.examination.common.interfaces.payment.response.CreatePayOrderResponse;
import com.xiaoyao.examination.mq.client.MQClient;
import com.xiaoyao.examination.mq.message.OrderCreatedMessage;
import com.xiaoyao.examination.order.domain.entity.Order;
import com.xiaoyao.examination.order.domain.enums.OrderStatus;
import com.xiaoyao.examination.order.domain.service.OrderDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DubboService
public class OrderServiceImpl implements OrderService {
    private final OrderDomainService orderDomainService;
    private final MQClient mqClient;
    private final StringRedisTemplate redisTemplate;

    @DubboReference
    private PayService payService;
    @DubboReference
    private DiscountService discountService;
    @DubboReference
    private GoodsService goodsService;

    public OrderServiceImpl(OrderDomainService orderDomainService, MQClient mqClient, StringRedisTemplate redisTemplate) {
        this.orderDomainService = orderDomainService;
        this.mqClient = mqClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserOrderSummaryResponse getUserOrderSummary(long userId) {
        List<Order> orders = orderDomainService.findAllOrderCountAndTotalByUserId(userId);
        int orderCount = 0;
        int goodsCount = 0;
        BigDecimal totalAmount = new BigDecimal("0.00");
        for (Order order : orders) {
            orderCount++;
            goodsCount += order.getCount();
            totalAmount = totalAmount.add(order.getTotal());
        }
        UserOrderSummaryResponse response = new UserOrderSummaryResponse();
        response.setOrderCount(orderCount);
        response.setGoodsCount(goodsCount);
        response.setTotalAmount(totalAmount.toString());
        return response;
    }

    @Transactional
    @Override
    public String submitOrder(long userId, long goodsId, int count, long globalId) {
        // 使用全局唯一的id来解决创建订单的幂等性
        if (Boolean.FALSE.equals(redisTemplate.opsForValue()
                .setIfAbsent("submit-order-id:" + globalId, "1", 5, TimeUnit.MINUTES))) {
            throw new ExaminationException(ErrorCode.ORDER_CREATED);
        }

        SubmitOrderGoodsInfoResponse goods = goodsService.getGoodsInfoInSubmitOrder(goodsId);
        if (goods == null) {
            throw new ExaminationException(ErrorCode.GOODS_NOT_FOUND);
        }

        Order order = new Order();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setName(goods.getName());
        order.setDescription(goods.getDescription());
        order.setImage(goods.getImage());
        order.setUnitPrice(goods.getCurrentPrice());
        order.setCount(count);
        // 计算总金额
        if (goods.getDiscountId() != null) {
            order.setTotal(discountService.compute(goods.getDiscountId(), goods.getCurrentPrice(), count));
        } else {
            order.setTotal(goods.getCurrentPrice().multiply(new BigDecimal(count)));
        }
        order.setSnapshotId(goods.getSnapshotId());
        order.setStatus(OrderStatus.PAY_WAITING.getStatus());
        // 创建支付订单
        CreatePayOrderResponse response = payService.createPayOrder(new CreatePayOrderRequest(
                order.getTotal().multiply(new BigDecimal(100)).setScale(0, RoundingMode.DOWN).intValue(),
                "购买体检套餐", PayService.PayType.ORDER));
        order.setPaymentCode(response.getPaymentCode());
        orderDomainService.save(order);

        // 发送定时消息到消息队列，如果超时未支付则关闭订单，超时时间为30分钟。
        mqClient.orderCreated(new OrderCreatedMessage(order.getPaymentCode()), 1000 * 60 * 30);

        return response.getPayUrl();
    }

    @Transactional
    @Override
    public void paySuccess(String paymentCode) {
        Order order = orderDomainService.findOrderByPaymentCode(paymentCode);
        // 使用CAS思想实现消息的幂等性
        if (tryPayOrder(order.getId())) {
            goodsService.increaseSalesVolume(order.getGoodsId(), order.getCount());
        }
    }

    /**
     * 使用CAS的方式尝试去更新订单状态为已支付。
     */
    private boolean tryPayOrder(long orderId) {
        return orderDomainService.updateStatus(orderId, OrderStatus.PAY_WAITING.getStatus(), OrderStatus.SUBSCRIBE_WAITING.getStatus());
    }

    @Override
    public boolean isPaid(long orderId) {
        // 先去数据库中查询订单是否已支付
        if (orderDomainService.isPaid(orderId)) {
            return true;
        }

        // 主动去支付服务中查询支付订单是否已完成支付
        boolean paid = payService.isPaid(orderDomainService.getPaymentCodeByOrderId(orderId));
        if (paid) {
            // 如果已支付，则更新订单状态为已支付。
            tryPayOrder(orderId);
        }
        return paid;
    }
}
