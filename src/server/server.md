

### Tomcat 请求过程

模拟 Tomcat 处理 Http 请求的过程：http://localhost:8080/webApplication/index

1. 服务器 8080 端口接收到客户端发来的请求，被一个在那监听的 HTTP1.1 的 Connector 获取链接请求
2. Connector 把请求交给同在 Service 的 Engine 处理，并等待 Engine 的响应
3. Engine 把 url 解析，把请求交给对应的 Host 处理，如果没有对应的 Host，则默认名为 localhost 的 Host 处理
4. Host 把 url 解析尾 webApplication/index，匹配 `context-path` 为 /webApplication 的 Context 处理（如果匹配不到的话，
将请求交给路径名为 "" 的 Context 去处理）
5. `context-path` 为 /webApplication 的 Context 会匹配 Servlet Mapping 为 /index 的 Servlet 处理
6. 构造 `HttpServletRequest` 和 `HttpServletResponse` 对象，作为参数调用 Servlet 的 `doGet` `doPost` 方法
7. 将 HttpServletResponse 一级一级返回, Context -> Host -> Engine -> Service -> Connector
8. Connector 把 HttpServletResponse 对象返回给客户端 browser