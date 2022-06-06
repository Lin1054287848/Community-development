package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class AlphaAspect {

    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")//com.nowcoder.community.service.*表示该包下的所有类 .*.*表示所有的方法(..) ..表示所有的参数
    public void pointcut(){ //切点

    }

    @Before("pointcut()") //声明在该切点之前 织入代码
    public void before(){
        System.out.println("before");
    }

    @After("pointcut()") //声明在该切点之后 织入代码
    public void after(){
        System.out.println("after");
    }

    @AfterReturning("pointcut()") //声明在该切点有了返回值之后 织入代码
    public void afterReturning(){
        System.out.println("afterReturning");
    }

    @AfterThrowing("pointcut()") //声明在该切点抛出异常的时候 织入代码
    public void afterThrowing(){
        System.out.println("afterThrowing");
    }

    @Around("pointcut()") ////声明在该切点之前和之后 都织入代码
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("around before");
        Object obj = joinPoint.proceed(); //调用目标组件的方法 //调用原始对象的方法
        System.out.println("around after");
        return obj;
    }
}
