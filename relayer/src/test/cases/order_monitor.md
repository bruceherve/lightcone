## 订单状态监控
---
测试订单状态的监控：

 - [过期订单](#expired)
 - [延迟生效订单](#pending-active)
 
---

### <a name="expired"></a>过期订单
---
分为： 

---

1. 测试订单过期的情况
    - **Objective**：测试测试提交订单之后，过了过期时间，是否将订单取消
    - **测试设置**：
        1. 设置新账号，并且有足够的余额和授权，1000LRC
        1. 下单为：Order(sell:100LRC,buy:1WETH,fee:10LRC，validSince：当前时间+10s), Order(sell:200,buy:1WETH,fee:10LRC,validSince:当前时间+1d) 
        2. sleep(20s), 等待监控逻辑执行
    - **结果验证**：
        1. **读取我的订单**：通过getOrders应该看到这两个订单，其中一个已过期取消，另一个为Pending
        2. **读取市场深度**：200LRC
        1. **读取我的成交**: 为空
        1. **读取市场成交**：为空
        1. **读取我的账号**: 账号1的余额分别为：LRC:`balance=1000,avaliableBalancwe=890`,
        
    - **状态**: Planned
    - **拥有者**: 红雨
    - **其他信息**：可参考原先的 EntryPointSpec_OrderStatusMonitorExpire
 
### <a name="pending-active"></a> 延迟生效的订单

---

1. 订单延迟生效的情况，
    - **Objective**：下一个在当前时间之后生效的订单，到了事件之后，订单需要生效并影响到orderbook
    - **测试设置和验证**：
        1. 设置新账户，余额都设置为1000LRC，授权为1000LRC
        2. sell:100LRC,buy:1WETH，validSince:当前时间+10s
        2. 依次上述提交订单 => 
        	- **验证**：
		        1. **返回结果**：提交成功
		        1. **读取我的订单**：getOrders都不为空，提交的所有订单都存在，并且状态都为Pending_Active
		        1. **读取市场深度**：为空
		        1. **读取我的成交**：为空
		        1. **读取市场成交**：为空
		        1. **读取我的账号**：余额不变
		3. sleep(20s),等待订单生效并监控到 =>
        	- **验证**：
		        1. **读取我的订单**：getOrders都不为空，但是卖单需要有一条为Pending
		        1. **读取市场深度**：不为空，100LRC
		        1. **读取我的成交**：为空
		        1. **读取市场成交**：为空
		        1. **读取我的账号**：可用金额减小100LRC
    - **状态**: Planned
    - **拥有者**: 红雨
    - **其他信息**：可参考原先的 EntryPointSpec_OrderStatusMonitorEffective

