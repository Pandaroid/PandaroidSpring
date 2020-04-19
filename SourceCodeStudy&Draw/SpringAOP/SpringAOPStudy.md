# SpringAOP Study

AOP（Aspect Oriented Programming）面向切面编程，可在不修改源码的情况下给程序统一动态添加功能。

- 预编译方式
- 运行期动态代理

AOP 可以看作是一种设计模式

- 追求调用者和被调用者之间的解耦
- 非业务类横切于业务类，如：
  - 日志
  - 事务
  - 安全

AOP 可将非业务的系统需求与业务需求分开

- 避免重复复制、粘贴非业务的系统需求代码
- 系统解耦、方便维护
- AOP 中最重要的设计模式：代理模式

## AOP 中必须明白的几个概念

统一概念术语，避免模糊含义，提高沟通效率。

### 1、切面（Aspect）

一个关注点的模块化，这个关注点可能会横切多个对象。

- 在 ApplicationContext 中通过 `<aop:aspect>` 来配置
- 在 Aspect 中会包含一些 Pointcut 和相应的 Advice
- 面向规则，具有相同规则的方法的集合体

Aspect 我理解为：它是描述在满足 Pointcut 定义的规则条件范围下，应该回调执行 Advice 中代码定义的操作。

实际 Spring 中：

- Advice 作用于 Joinpoint ，被织入相应的 Joinpoint
- 而具体是织入哪些 Joinpoint ？则由 Pointcut 来限定具体的 Joinpoint 集合

#### 连接点（Joinpoint）

程序执行过程中的某一行为，例如：

- MemberService.get() 的调用
- MemberService.delete() 抛出异常

这些 Joinpoint 是具体被匹配符合 Pointcut 规则并添加织入 Advice 的地方。

我理解为：程序执行过程中的某一个行为发生后的特定的位置（通常是拥有边界性质的特定位置，特定位置我理解为点），符合 Pointcut 匹配条件的 Joinpoint 就会织入相应的 Advice 。

这些特定位置点，比如：

- 某个方法调用前
- 某个方法调用后
- 某个方法执行抛出异常后
- 某个方法正常返回后

Joinpoint 就是所有可以织入 Advice 的特定位置，而把某些符合 Pointcut 的 Joinpoint 进行织入 Advice ，就可以对原有的代码逻辑进行增强，具体在程序中就是执行到符合条件的 Joinpoint 会通知回调我们的 Advice 中的代码逻辑。

Joinpoint 存在于 Target Object 上，Target Object 是被代理的对象。

### 2、通知 / 增强（Advice）

Aspect 对于某一个 Joinpoint 产生的动作，一个 Aspect 可以包含多个 Advice ，对满足 Pointcut 规则条件的 Joinpoint 织入 Advice 。

Advice 的类型：

- 前置（Before Advice）：在 Joinpoint 前被执行的 Advice 
  - 它并不能阻止 Joinpoint 的执行，除非发生了异常
    - 在 Before Advice 的代码中，不能人为决定是否继续执行 Joinpoint 中的代码
  - ApplicationContext 中在 `<aop:aspect>` 里面使用 `<aop:before>` 元素进行声明
- 后置（After Advice）：Final Advice ，无论一个 Joinpoint 是正常退出还是发生了异常（Joinpoint 退出的时候），都会被执行的 Advice 
  - ApplicationContext 中在 `<aop:aspect>` 里面使用 `<aop:after>` 元素进行声明
- 返回后（After Return Advice）：在一个 Joinpoint 正常完成后（返回且返回值为非 Void ，没有抛出异常），会被执行的被织入的 Advice 
  - ApplicationContext 中在 `<aop:aspect>` 里面使用 `<aop:after-returning>` 元素进行声明
- 环绕包围型（Around Advice）：包围一个 Joinpoint ，最常用的 Advice ，类似 Web 中 Servlet 规范中的 Filter 的 doFilter 方法。
  - 可以在方法的调用前后完成自定义的行为，也可以选择不执行
    - 只要触发 Joinpoint 的调用，在 Joinpoint 前和退出后都会执行的 Advice（也可以忽略不执行）
  - ApplicationContext 中在 `<aop:aspect>` 里面使用 `<aop:around>` 元素进行声明
- 异常后（After Throwing Advice）：Joinpoint 方法抛出异常后执行的 Advice 
  - ApplicationContext 中在 `<aop:aspect>` 里面使用 `<aop:after-throwing>` 元素进行声明

可以将多个 Advice 应用到同一个 Target Object 上

- 即可以将多个 Aspect 应用到同一 Target Object
  - 对于被应用的同一 Target Object ，其符合 Pointcut 的所有 Joinpoint 都会被织入对应的 Advice
  - 执行时，调用 Target Object 的相应方法，触发 Joinpoint 上被织入的 Advice ，进而去通知执行相应的增强逻辑代码

### 3、切入点（Pointcut）

匹配 Joinpoint 的断言。我们不是要在所有的 Joinpoint 上进行 Advice 的织入，因此通过 Pointcut 可以提供一组匹配规则来匹配我们想要织入 Advice 的 Joinpoint 。

- 在 Spring AOP 中 Advice 和一个 Pointcut 表达式关联
- Aspect 中所有 Advice 所关注的 Joinpoint ，都由 Pointcut 表达式来决定
- 可以匹配一组 Joinpoint ，然后将相应的 Advice 织入进行增强
- 由 Spring AOP 的规则解析引擎负责解析 Pointcut 的匹配规则，进而定位具体的 Joinpoint 集合

### 4、目标对象（Target Object）

被一个或者多个 Aspect 织入 Advice 的目标对象。

- 例如：具体的 AServiceImpl 和 BServiceImpl 。
- 实际运行时，Spring AOP 采用代理实现，实际 AOP 操作的是 Target Object 的代理对象

### 5、AOP 代理（AOP Proxy）

在 Spring AOP 中有两种代理方式：

- JDK 动态代理
- CGLib 代理
  - Code Generation Library
  - 强大、高性能、高质量的 Code 生成类库
  - 可以在运行期扩展 Java 类、实现 Java 接口
  - 底层使用 ASM ：一个小而快的字节码处理框架，用于转换字节码并生成新的类

默认情况下，Spring AOP 实现代理的方式：

- Target Object 实现了接口时，则采用 JDK 动态代理
  - 此时，可以强制使用 CGLib 动态代理
    - 添加 CGLib 库：SPRING_HOME/cglib/*.jar（aspectjrt-xxx.jar 、aspectjweaver-xxx.jar 、cglib-nodep-xxx.jar）
    - 在 Spring 配置文件中加入 `<aop:aspectj-autoproxy proxy-target-class="true" />` 
- 反之（Target Object 没有实现接口），则采用 CGLib 动态代理。

Spring 会自动在 JDK 动态代理和 CGLib 动态代理之间转换。

- JDK 动态代理只能对实现了接口的类生成代理
  - CGLib 针对类实现代理（不管有没有实现接口），对指定的类生成一个子类，覆盖其中的方法
    - 因为是继承，所以该类、及其方法，都不要声明成 final ，不然无法继承
- JDK 动态代理利用拦截器和反射机制
  - 拦截器必须实现 InvocationHandler 接口
    - 反射机制获取 Target Object 的所有接口
    - 生成一个实现 Target Object 接口的匿名代理类
    - 在调用具体方法前调用 InvocationHandler 来处理
  - CGLib 动态代理是利用 ASM 开源包，加载 Target Object 的 Class 文件，修改其字节码生成子类

动态代理使得我们免于重写接口中的方法，可以重点关注扩展相应的功能、增强 Target Object 中符合 Pointcut 的 Joinpoint ，处理非业务的公共的系统逻辑。

由上可知，Spring AOP 的选择是：能使用 JDK 动态代理的情况下，优先使用 JDK 动态代理，否则，才使用 CGLib 动态代理；或者，用户配置指定强制使用 CGLib 动态代理。为什么？

- 因为 JDK 动态代理的效率随着 JDK 的更新会越来越高，但是 CGLib 的效率增长速度已经远远落后于 JDK 动态代理了，并且在执行代理方法的次数较少时，JDK 动态代理效率会更高
- CGLib 动态代理的优势：
  - 底层采用 ASM 字节码生成框架，使用字节码技术生成代理类
  - 在 JDK 6 之前比使用 Java 反射效率要高
- CGLib 的原理是动态生成被代理类的子类，所以不能对生命为 final 的类和方法进行代理
- 但是，随着 JDK 的不断升级，CGLib 的代理效率逐渐跟不上 JDK 动态代理
  - 在 JDK 6 、JDK 7 、JDK 8 逐步对 JDK 动态代理进行优化以后，在调用次数较少的情况下，JDK 代理效率高于 CGLib 代理效率
  - 只有当进行大量调用的时候，JDK 6 和 JDK 7 的代理效率比 CGLib 略低一点
    - 但是 JDK 8 以后，JDK 动态代理效率高于 CGLib

JDK 动态代理不需要第三方库支持，在 JDK 环境中即可进行代理：

- 实现 InvocationHandler
- 使用 Proxy.newProxyInstance() 产生代理对象
- 被代理的对象必须要实现接口

CGLib 必须依赖于 CGLib 的类库

- 不需要类实现任何接口
- 通过为 Target Object 生成一个子类并覆盖其中的方法来进行代理
- 属于继承的方式

优先使用 JDK 动态代理，从执行效率和优化潜力、以及面向接口编程来说，JDK 动态代理都是首选。