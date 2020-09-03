package com.yang.framework.annotation;

/**
 * @author yzy
 * @date 2020/8/24
 * @describe
 */
public class Test {
    public static void main(String[] args) {
        String simpleName = "IocConfig";
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        String.valueOf(chars);
        System.out.println(chars);
    }
}
