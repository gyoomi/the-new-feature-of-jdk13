## JDK13新特性理解与使用

 - 350: Dynamic CDS Archives
 - 351: ZGC: Uncommit Unused Memory
 - 353: Reimplement the Legacy Socket API
 - 354: Switch Expressions (Preview)
 - 355: Text Blocks (Preview)

官方网站： http://openjdk.java.net/projects/jdk/13/

### Dynamic CDS Archives

了解这个特性之前，需要先了解一下跟它有很大关联的特性JEP310：Application Class-Data Sharing，简称AppCDS。这个特性简介就是为了改善JVM应用的启动速度和内存占用，并且扩展了CDS（Class-Data Sharing）特性从而允许应用的类也可以被放置在共享的归档类（archived classes）文件中。这个JEP310的主要目标如下：

1. 通过共享不同Java进程之间通用的类元数据从而减少内存占用；
2. 改进启动时间；
3. 扩展CDS从而允许归档类被加载到自定义类加载器中；
4. 扩展CDS允许归档类来自JDK运行时镜像文件（$JAVA_HOME/lib/modules）；

成功参考指标：

1. 多JVM进程能够节省很大的内存空间；
2. 进程的启动时间提升明显；

JEP350特性期望扩展CDS，从而允许在Java应用执行后进行动态类归档，归档的类将包括当前默认基础CDS归档中不存在的应用类和库中的类。这个特性的主要目标有

1. 提高CDS的可用性，消除了用户使用时为每个应用程序创建类列表（class list）的需要；
2. 通过-Xshare:dump参数开启静态归档，包括内建的类加载器和用户自定义的类加载器。

在这之前，如果Java应用要使用CDS的话，3个步骤是必须的：

1. 执行一次或者多次试运行，从而创建一个class list；
2. 通过使用创建的class list来dump一个归档（archive）；
3. 用这个归档来运行；

```java
# JVM退出时动态创建共享归档文件
bin/java -XX:ArchiveClassesAtExit=hello.jsa -cp hello.jar Hello

# 用动态创建的共享归档文件运行应用
bin/java -XX:SharedArchiveFile=hello.jsa -cp hello.jar Hello
```

### ZGC: Uncommit Unused Memory

增强ZGC特性，将没有使用的堆内存归还给操作系统。ZGC当前不能把内存归还给操作系统，即使是那些很久都没有使用的内存，有点像貔貅一样，只进不出，哈哈。这种行为并不是对任何应用和环境都是友好的，尤其是那些内存占用敏感的服务，例如：

1. 按需付费使用的容器环境；
2. 应用可能长时间闲置，并且和很多其他应用共享和竞争资源的环境；
3. 应用在执行期间有非常不同的堆空间需求，例如，可能在启动的时候比稳定运行的时候需要更多的内存。

HotSpot的G1和Shenandoah这两个GC已经提供了这种能力，并且对某些用户来说，非常有用。因此，把这个特性引入ZGC会得到这些用户的欢迎

ZGC的堆又若干个Region组成，每个Region被称之为ZPage。每个Zpage与数量可变的已提交内存相关联。当ZGC压缩堆的时候，ZPage就会释放，然后进入page cache，即ZPageCache。这些在page cache中的ZPage集合就表示没有使用部分的堆，这部分内存应该被归还给操作系统。回收内存可以简单的通过从page cache中逐出若干个选好的ZPage来实现，由于page cache是以LRU顺序保存ZPage的，并且按照尺寸（小，中，大）进行隔离，因此逐出ZPage机制和回收内存相对简单了很多，主要挑战是设计关于何时从page cache中逐出ZPage的策略。

一个简单的策略就是设定一个超时或者延迟值，表示ZPage被驱逐前，能在page cache中驻留多长时间。这个超时时间会有一个合理的默认值，也可以通过JVM参数覆盖它。Shenandoah GC用了一个类型的策略，默认超时时间是5分钟，可以通过参数-XX:ShenandoahUncommitDelay = milliseconds覆盖默认值。

像上面这样的策略可能会运作得相当好。但是，用户还可以设想更复杂的策略：不需要添加任何新的命令行选项。例如，基于GC频率或某些其他数据找到合适超时值的启发式算法。JDK13将使用哪种具体策略目前尚未确定。可能最初只提供一个简单的超时策略，使用**-XX:ZUncommitDelay = seconds**选项，以后的版本会添加更复杂、更智能的策略（如果可以的话）。

uncommit能力默认是开启的，但是无论指定何种策略，ZGC都不能把堆内存降到低于Xms。这就意味着，如果Xmx和Xms相等的话，这个能力就失效了，-XX:-ZUncommit这个参数也能让这个内存管理能力失效。

### Reimplement the Legacy Socket API

用一个易于维护和Debug的，更简单、更现代的实现来取代java.net.Socket和java.net.ServerSocket。Socket和ServerSocket可以追溯到JDK1.0，它们的实现混合了Java和C代码，维护和调试都非常痛苦。而且其实现用线程栈来进行IO buffer，导致某些场景需要调大Xss。

全新实现的NioSocketImpl，用来取代PlainSocketImpl，它的优点如下：

1. 非常容易维护和Debug；
2. 直接使用JDK的NIO实现，不需要自己的本地代码；
3. 结合了buffer cache机制，所以不需要用线程栈来进行IO操作；
4. 用JUC的锁取代synchronized修饰的方法；


### Switch Expressions (Preview)

**这是对我们平时写代码的习惯影响较大的一个新特性。**


扩展Switch表达式，既能用陈述的方式，也能用表达式的方式。并且这两种形式都可以用传统方式（case ... : labels），或者新的方式（case ... -> labels），并且还准备引入表达式匹配（JEP305），类似这种玩法

```java
if (obj instanceof String s && s.length() > 5) {
    .. s.contains(..) ..
}
```

Switch表达式最初在JEP325中被提出，在JDK12中作为预览特性，根据反馈，这次的JEP354相比JEP325有一些改变，新版Switch表达式用法参考如下：


```java
public class SwitchDemo {

    public static void main(String[] args) {
        // JDK13的写法1
        // 如果没有default 则不进行任何输出
        // 动态修改variable的看打印的效果
        int variable = new Integer(args.length < 1 || args[0] == null ? "0" : args[0]);
        switch (variable) {
            case 1, 2, 3 -> System.out.println("小");
            case 4, 5, 6 -> System.out.println("中");
            case 7, 8, 9 -> System.out.println("大");
            default -> System.out.println("超出范围，无法识别");
        }

        System.out.println("***************************");
        // JDK13写法2（带返回值）
        // 动态修改variable2的看打印的效果
        int variable2 = 5;
        String result = switch (variable2) {
            case 1, 2, 3 -> "小";
            case 4, 5, 6 -> "中";
            case 7, 8, 9 -> "大";
            default -> "超出范围，无法识别";
        };
        System.out.println(result);

        System.out.println("***************************");
        // JDK13,JDK12之前的古老写法
        // 动态修改variable3的看打印的效果
        int variable3 = 8;
        switch (variable3) {
            case 1,2,3:
                System.out.println("小3");
                // 必须要加上break，而JDK12,JDK13中则不需要
                break;
            case 4,5,6:
                System.out.println("中3");
                break;
            case 7,8,9:
                System.out.println("大3");
                break;
            default:
                System.out.println("其他意外情况3");
        }
    }
}
```

### Text Blocks (Preview)

文本块

这个特性对我们的帮助的确太大了，点赞！！！

之前的写法的中，我们一般使用如下形式拼接较长的字符串。

```java
        String sql = " SELECT "
                   + "     t.* "
                   + " FROM user t";

        String html = "<html>"
                    + "    <head></head>"
                    + "    <body>"
                    + "        <font color='red'>测试字符</font>"
                    + "    </body>"
                    + "</html>";
        // JEP326使用**`**这个符号
        String html = `<html>
                           <body>
                               <p>Hello World.</p>
                           </body>
                       </html>
                      `;
```

而新的文本块使用`"""`这个符号，进行拼接。

新版本文本块特性的目标：

1. 简化表达多行字符串，不需要转义；
2. 增强可读性；

```java13
        // JDK13的写法
        String sql13 = """
                        SELECT
                           t.*
                        FROM user t
                       """;

        String html13 = """
                           <html>
                                <head></head>
                                <body>
                                    <font color='red'>测试字符</font>
                                </body>
                           </html>
                        """;
```





























