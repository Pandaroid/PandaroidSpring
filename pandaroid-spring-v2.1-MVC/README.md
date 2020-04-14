# MVC 九大组件

- MultipartResolver ：多文件上传组件
- LocalResolver ：本地语言环境
- ThemeResolver ：主题模板处理器
- HandlerMapping ：保存 url 和 handler 的映射关系
- HandlerAdapter ：动态参数适配器
  - 动态匹配参数，将 url 中的 String 参数适配为各种类型
    - Integer 、Double 、Boolean ...
    - 对象
  - 动态匹配、动态转化参数，根据用户定义的类型自动转型
- HandlerExceptionResolver ：异常拦截器
- RequestToViewNameTranslator ：视图提取器，从 request 中获取 viewName
- ViewResolver ：视图转换器，模板引擎
- FlashMapManager ：参数缓存器
  - 请求转发或重定向时，将参数带过去