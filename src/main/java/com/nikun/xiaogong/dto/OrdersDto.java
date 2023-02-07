package com.nikun.xiaogong.dto;

import com.nikun.xiaogong.entity.OrderDetail;
import com.nikun.xiaogong.entity.Orders;
import lombok.Data;
import java.util.List;

@Data
public class OrdersDto extends Orders {

    private List<OrderDetail> orderDetails;
	
}
