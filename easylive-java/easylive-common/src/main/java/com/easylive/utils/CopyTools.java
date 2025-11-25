package com.easylive.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * CopyTools
 * 基于 Spring 的 BeanUtils 工具类，封装了对象属性拷贝的常用方法。
 * 用途：把一个对象（或对象集合）的同名属性值复制到另一个对象（或集合）。
 */
public class CopyTools {

    /**
     * 将一个 List<S> 转换为 List<T>
     * @param sList   源对象列表
     * @param classz  目标对象的 Class，用于反射创建实例
     * @param <T>     目标对象类型
     * @param <S>     源对象类型
     * @return        复制后的目标对象列表
     */
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                // 使用反射创建目标对象实例（必须有无参构造函数）
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace(); // 出现异常时打印堆栈
            }
            // 复制属性：s → t（只复制同名且类型兼容的属性）
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    /**
     * 将单个对象 S 拷贝为目标对象 T
     * @param s      源对象
     * @param classz 目标对象的 Class
     * @param <T>    目标对象类型
     * @param <S>    源对象类型
     * @return       复制后的目标对象
     */
    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.newInstance(); // 反射创建目标对象
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s, t); // 复制属性
        return t;
    }

    /**
     * 将源对象属性拷贝到已有的目标对象
     * @param s 源对象
     * @param t 目标对象（已存在实例）
     * @param <T> 目标对象类型
     * @param <S> 源对象类型
     */
    public static <T, S> void copyProperties(S s, T t) {
        BeanUtils.copyProperties(s, t);
    }
}
