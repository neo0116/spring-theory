package com.byteDance.spring;

import com.byteDance.spring.annotation.BDController;
import com.byteDance.spring.annotation.BDService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BDDispatcherServlet extends HttpServlet {

    //init-param
    private static final String CONTEXTCONFIGLOCATION = "contextConfigLocation";

    private final static String[] digits = {
            "A" , "B" , "C" , "D" , "E" , "F" ,
            "G" , "H" , "I" , "J" , "K" , "L" ,
            "M" , "N" , "O" , "P" , "Q" , "R" ,
            "S" , "T" , "U" , "V" , "W" , "X" ,
            "Y" , "Z"
    };

    private static Properties properties = new Properties();

    private static List<String> classPath = new ArrayList<>(16);

    private static Map<String, Object> ioc = new ConcurrentHashMap<>(16);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        doLoadingConfigFile(config);

        doClassScanner(properties.getProperty("packageScanner"));

        initIOC();

        doDependancyInjection();

        initHandlermapping();

    }

    private void initHandlermapping() {
    }

    private void doDependancyInjection() {


    }

    private void initIOC() {
        for (String path : classPath) {
            try {
                Class<?> clazz = Class.forName(path);
                if (clazz.isAnnotationPresent(BDController.class)) {
                    String value = clazz.getAnnotation(BDController.class).value();
                    Object instance = clazz.newInstance();
                    if (!"".equals(value)) {
                        ioc.put(value, instance);
                        continue;
                    }
                    value = toLowercase(clazz.getSimpleName());
                    ioc.put(value, instance);
                } else if (clazz.isAnnotationPresent(BDService.class)) {
                    String value = clazz.getAnnotation(BDService.class).value();
                    Object instance = clazz.newInstance();
                    if (!"".equals(value)) {
                        ioc.put(value, instance);
                        continue;
                    }
                    value = toLowercase(clazz.getSimpleName());
                    ioc.put(value, instance);

                    Class<?>[] clazzInterfaces = clazz.getInterfaces();
                    for (Class<?> clazzInterface : clazzInterfaces) {
                        String iName = toLowercase(clazzInterface.getSimpleName());
                        if (ioc.containsKey(iName)) {
                            throw new RuntimeException(iName + "已经存在");
                        }
                        ioc.put(iName, instance);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //首字母转换成小写
    private String toLowercase(String name) {
        char[] c = name.toCharArray();
        System.out.println(Arrays.toString(c));
        if (Arrays.asList(digits).contains(String.valueOf(c[0]))) {
            c[0] += 32;
        }
        return String.valueOf(c);
    }

    private void doClassScanner(String packageName) {
        try {
            URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
            File file = new File(url.getFile());
            if (file.exists()) {
                //文件列表
                File[] files = file.listFiles();
                for (File f : files) {
                    //如果是目录就递归
                    if (f.isDirectory()) {
                        doClassScanner(packageName + "." + f.getName());
                    } else {
                        if (!f.getName().endsWith(".class")) {continue;}
                        classPath.add(packageName + "." + f.getName().replace(".class", ""));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doLoadingConfigFile(ServletConfig config) {
        String configInitParameter = config.getInitParameter(CONTEXTCONFIGLOCATION);
        String replace = configInitParameter.replace("classpath:", "");
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(replace);
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
