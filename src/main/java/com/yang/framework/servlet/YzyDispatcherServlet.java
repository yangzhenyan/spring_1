package com.yang.framework.servlet;

import com.yang.framework.annotation.YAutowired;
import com.yang.framework.annotation.YController;
import com.yang.framework.annotation.YRequestMapping;
import com.yang.framework.annotation.YService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author yzy
 * @date 2020/8/23
 * @describe
 */
public class YzyDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    //2、初始化ioc容器
    private Map<String, Object> iocMap = new HashMap<>();
    //存放全类名
    private List<String> classNameList = new ArrayList<>();
    //handleMapping集合
    private Map<String, Method> handleMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //7、执行相关方法
        try {
            String requestURI = req.getRequestURI();
            //获取项目名 "/项目名"
            String contextPath = req.getContextPath();
            //获取请求的uri
            String uri = requestURI.replace(contextPath, "");

            //如果请求的uri不包含在handleMapping集合
            if (!handleMap.containsKey(uri)) {
                resp.getWriter().write("404 Not Found !!");
                return;
            }
            Method method = handleMap.get(uri);
            Map<String, String[]> params = req.getParameterMap();
            String beanName = toLowerLetters(method.getDeclaringClass().getSimpleName());
            method.invoke(iocMap.get(beanName),new Object[]{req,resp,params.get("name")[0],params.get("id")[0]});
        } catch (Exception e) {
            resp.getWriter().write("500 Internal Server Exception !!");
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、读取配置文件
        doReadConfig(config.getInitParameter("myConfigLocation"));
        //3、扫描包路径下的类  将.替换成/
        String scanPackage = properties.getProperty("scanPackage").trim();
        doScanner(scanPackage.replaceAll("\\.", "/"));
        //4、创建实例化对象将其注入到ioc容器中
        doInstance();
        //5、DI 扫描容器中对象 对其属性赋值
        doAutowired();
        //6、初始化handleMapping 将url和method一一映射
        doHandleMapping();

        System.out.println("Spring Framework init......");
    }

    //1、读取配置文件
    private void doReadConfig(String configLocation) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(configLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //3、扫描包路径下的类
    private void doScanner(String scanPackage) {
//        scanPackage = scanPackage.replace(".", "/");

        URL url = getClass().getClassLoader().getResource("/" + scanPackage);
        //url.getFile() 获取此文件名URL
        File filePath = new File(url.getFile());
        File[] files = filePath.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                //如果是文件夹 则在该文件夹的路径下递归扫描
                String s = scanPackage + "/" + file.getName();
                doScanner(s);
            }
            //再判断是不是以.class结尾的文件
            if (file.getName().endsWith(".class")) {
                //拼接全类名 去除.class后缀
                String className = scanPackage.replace("/", ".") + "." + file.getName().replace(".class", "");
                classNameList.add(className);
            }
        }
    }

    //4、实例化对象并注入ioc中
    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        //遍历全类名list
        for (String name : classNameList) {
            try {
                //根据全类名获取对应字节码对象

                Class<?> aClass = Class.forName(name);
                //判断是不是加了@YService或@YController注解
                if (!aClass.isAnnotationPresent(YService.class) & !aClass.isAnnotationPresent(YController.class)) {
                    continue;
                }
                //如果是接口上加的注解 则不放入ioc
                if (aClass.isInterface()) {
                    continue;
                }

                //将对象放入ioc中  ioc中组件名默认首字母小写
                //如果不同包下有相同类名 自定义类名
                String beanName = null;
                if (aClass.isAnnotationPresent(YService.class)) {
                    beanName = toLowerLetters(aClass.getSimpleName());
                    if (!"".equals(aClass.getAnnotation(YService.class).value())) {
                        beanName = aClass.getAnnotation(YService.class).value();
                    }

                } else if (aClass.isAnnotationPresent(YController.class)) {
                    beanName = toLowerLetters(aClass.getSimpleName());
                    if (!"".equals(aClass.getAnnotation(YController.class).value())) {
                        beanName = aClass.getAnnotation(YController.class).value();
                    }
                }

                //获取对象
                Object instance = aClass.newInstance();
                iocMap.put(beanName, instance);

                //获得这个对象实现的所有接口 将其接口也放入ioc 接口的值为子类值
                for (Class<?> inter : aClass.getInterfaces()) {
                    beanName = toLowerLetters(inter.getSimpleName());
                    iocMap.put(beanName, instance);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //5、DI 扫描容器中对象 对其属性赋值
    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            //获取对象中的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //如果加了@YAutowired注解就给属性赋值
                if (field.isAnnotationPresent(YAutowired.class)) {

                    //暴力访问
                    field.setAccessible(true);
                    try {
                        //获取该属性beanName
                        String beanName = toLowerLetters(field.getType().getSimpleName());
                        field.set(entry.getValue(), iocMap.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

            }
        }
    }

    //6、初始化handleMapping 将url和method一一映射
    private void doHandleMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            if (!aClass.isAnnotationPresent(YController.class)) {
                continue;
            }
            //判断类路径上是否有@YRequestMapping注解 有就获取
            String baseUrl = null;
            if (aClass.isAnnotationPresent(YRequestMapping.class)) {
                baseUrl = aClass.getAnnotation(YRequestMapping.class).value();
            }
            //判断方法上@YRequestMapping注解
            Method[] methods = aClass.getMethods();
            String methodUrl = null;
            for (Method method : methods) {
                if (method.isAnnotationPresent(YRequestMapping.class)) {
                    methodUrl = method.getAnnotation(YRequestMapping.class).value();
                    String uri = "/" + baseUrl + "/" + methodUrl;
                    //url method放入集合
                    handleMap.put(uri, method);
                }
            }

        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    //获取对象名并把首字母转为小写
    private String toLowerLetters(String simpleName) {
        String letters = simpleName.substring(0, 1).toLowerCase();
        return letters + simpleName.substring(1);
    }
}
