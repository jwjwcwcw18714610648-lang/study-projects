package com.easylive.annotation;

import com.easylive.entity.enums.MessageTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordUserMessage {
    MessageTypeEnum messageType();
}
//@Target({ElementType.METHOD, ElementType.TYPE})
//我的注解只能写在 方法 或 类/接口 上面；写在字段、参数、构造器等其他地方会直接编译报错。
//@Retention(RetentionPolicy.RUNTIME)
//我的注解不仅要在源码里保留，还要编译进 .class 文件，并且运行时仍能通过反射读到它。
//如果换成 SOURCE 或 CLASS，运行时就 getAnnotation 不到了，AOP 也就切不到