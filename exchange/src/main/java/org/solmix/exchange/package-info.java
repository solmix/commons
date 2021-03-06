/**
 * 通用消息模型(Messaging Model)
 * 
 * 抽象服务模型(Abstract Service Model) 
 * 1,消息(Message),定义合法的消息结构和约束.
 * 2,操作(Operation),与服务进行交互的一次操作.
 * 3,消息交换类型(ExchangeType).
 * 4,服务(service)一组相关联的操作的集合.
 * 
 * 具体服务模型(Concrete Service Model)
 * 具体服务模型建立在抽象服务模型之上，
 * 1,绑定(Protocol)服务所绑定的协议类型.
 * 2,端点（Endpoints）：为服务消费者指明通过特定协议与服务提供者交互所需的通信端 点的信息。
 *   端点是一种形式上的标识，其内部使用的协议是基于Java标准消息契约，与通常的通信协议无关
 * 3,服务（Service）：提供访问该服务的一组端点的集合，一个服务实现了特定的服务类 型（接口）。
 *   通常，一个端点通过结合其服务名称和端点名称来识别，该结合称之为服务端点（Service Endpoint）。
 *   
 *通用消息交换模型:消费者组件生成服务请求,把消息传送到提供者组件.
 *
 *一个消费者（SE/BC）创建一个规格化消息NM并将其放入一个新的消息交换(MessageExchange)中.
 *该消息交换的地址被设定为一个服务端点（ServiceEndpoint），服务引擎未指定用哪一个组件来处理该服务请求。
 * 
 * 服务的消费者和提供者    (Service Consumers and Providers) 
 * SE和绑定组件可以作为服务消费者，服务提供者或两者兼具。
 * 服务提供者通过端点（endpoint）提供WSDL描述的服务；
 * 服务消费者发送消息交换调用特定的操作来使用服务。服务（service）实现了服务接口，
 * 服务接口是通过交换抽象定义的消息来描述的一组相关的操作
 * 
 * 一条规格化消息由以下三个主要部分组成：
 *· 消息载荷（payload）：符合抽象WSDL消息类型，不具有任何协议编码或格式信息的XML文档（这不是一个消息的规范格式）。 
 *· 消息属性（或元数据）：包含了在消息处理过程中获得的与消息相关的额外信息。消息属性可以包含消息的安全信息（例如消息接收方的签名信息），事务上下文信息和组件特定的信息。  
 *. 消息附件：消息载荷（内容）的一部分可以由附件构成，附件由消息载荷来引用，并且附件中包含一个可以操作附件内容的数据处理器。这些附件可以是非XML格式的。
 * 
 * 传输通道(pipeline)
 * 
 * 服务调用和消息交换模式(Service Invocation and Message Exchange Patterns)
 * .单向交互（One-Way）模式：服务消费者向提供者发送一个请求，服务提供者不必向消费者返回任何错误（故障）信息。  
 * ·可靠的单向交互（Reliable One-Way）模式：服务消费者向提供者发送一个请请求求。如果服务提供者处理该请求失败，会向消费者返回出错信息-
 * 回复（Request-Response）模式：服务消费者向提供者发送一个请求，并期望服务提供者响应。如果处理请求失败则返回出错信息。  ·           
 * 请求-选择回复（Request Optional-Response）模式：服务消费者向提供者发送一个请求，并期望服务提供者响应。消费者和提供者接受到一个消息后，都可以选择是否应答。  
 * 上述提到的消费者和提供者角色可能是绑定组件也可能是服务引擎。
 * 当一个绑定组件作为服务消费者时，说明存在一个外部的服务消费者；
 * 同样，当一个绑定组件作为服务提供者时，说明存在一个外部的服务提供者。
 * 当服务引擎作为服务提供者或消费者时，说明该服务引擎是一个内部参与者
 * 
 * 消息交换（ME）作为规格化消息的―容器（container）‖。
 * ME不仅封装了其实现的消息交换模型中的输入（in）消息和输出（out）消息，
 * 它还包含了这些消息的元数据信息以及正在进行的消息交换的状态信息。消息交换代表了JBI本地服务调用的一部分。
 * 下表说明了服务调用和消息交换模式之间的关系
 * 
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2014-10-3
 */

package org.solmix.exchange;