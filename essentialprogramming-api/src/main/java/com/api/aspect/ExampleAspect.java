package com.api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

//delete later
@Aspect
@Component
public class ExampleAspect {

    //@Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    @Around("com.config.AOPConfig.serviceOperation())")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        CodeSignature signature = (CodeSignature) joinPoint.getSignature();

        System.out.println("Method name:" + signature.getName());
        // Method args
        System.out.println("Method args names:");
        Arrays.stream(signature.getParameterNames())
                .forEach(s -> System.out.println("arg name: " + s));

        System.out.println("Method args types:");
        Arrays.stream(signature.getParameterTypes())
                .forEach(s -> System.out.println("arg type: " + s));

        System.out.println("Method args values:");
        Arrays.stream(joinPoint.getArgs())
                .forEach(o -> System.out.println("arg value: " + (o != null ? o.toString() : "")));

        return joinPoint.proceed();
    }
}
