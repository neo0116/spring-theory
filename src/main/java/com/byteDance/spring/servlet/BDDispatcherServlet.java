package com.byteDance.spring.servlet;

import com.byteDance.spring.annotation.*;
import com.byteDance.spring.bean.Handlermapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BDDispatcherServlet extends HttpServlet {

    //init-param
    private static final String CONTEXTCONFIGLOCATION = "contextConfigLocation";

    private final static String[] digits = {
            "A", "B", "C", "D", "E", "F",
            "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X",
            "Y", "Z"
    };

    private static Properties properties = new Properties();

    //类路径
    private static List<String> classPath = new ArrayList<>(16);

    //ioc容器
    private static Map<String, Object> ioc = new ConcurrentHashMap<>(16);

    //handlermapping, String中用的是一个List<Handlermapping>，可能它觉得在map中用key存url冗余了吧
    private static Map<String, Handlermapping> handlermappings = new ConcurrentHashMap<>(16);


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //uri
            String requestURI = req.getRequestURI();
            //项目路径
            String servletPath = req.getServletPath();
            String url = requestURI.replace(servletPath, "");
            Handlermapping handlermapping = handlermappings.get(url);
            if (handlermapping == null) {
                resp.getWriter().write("{code:404,msg:'未找到请求资源'}");
                return;
            }
            Method method = handlermapping.getMethod();
            Object controller = handlermapping.getController();
            Map<String, Integer> paramSortMap = handlermapping.getParamSortMap();

            Map<String, String[]> parameterMap = req.getParameterMap();
            Class<?>[] parameterTypes = handlermapping.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            //请求参数
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                if (paramSortMap.containsKey(key)) {
                    Integer index = paramSortMap.get(key);
                    Class<?> parameterType = parameterTypes[index];
                    String value = entry.getValue()[0];
                    args[index] = converter(parameterType, value);
                }
            }
            //形参是否有HttpServletRequest
            String servletRequestTypeName = HttpServletRequest.class.getTypeName();
            if (paramSortMap.containsKey(servletRequestTypeName)) {
                args[paramSortMap.get(servletRequestTypeName)] = req;
            }
            //形参是否有HttpServletResponse
            String servletResponseTypeName = HttpServletResponse.class.getTypeName();
            if (paramSortMap.containsKey(servletResponseTypeName)) {
                args[paramSortMap.get(servletResponseTypeName)] = resp;
            }

            Object invoke = method.invoke(controller, args);
            resp.getWriter().write(invoke.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Object converter(Class<?> parameterType, String value) {
        //spring的convert可以自定义，具体可以看Converter接口
        if (parameterType == Integer.class) {
            return Integer.parseInt(value);
        } else if (parameterType == Double.class) {
            return Double.parseDouble(value);
        } else {
            return value;
        }
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
        if (!ioc.isEmpty()) {
            try {
                for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                    Object singleton = entry.getValue();
                    Class<?> clazz = singleton.getClass();
                    //有没有@BDController
                    if (!clazz.isAnnotationPresent(BDController.class)) {
                        continue;
                    }
                    //提取baseUrl
                    String baseUrl = "";
                    if (clazz.isAnnotationPresent(BDRequestMapping.class)) {
                        BDRequestMapping bdRequestMapping = clazz.getAnnotation(BDRequestMapping.class);
                        String value = bdRequestMapping.value();
                        baseUrl = "".equals(value) ? "" : ("/" + value).replaceAll("//", "/");
                    }
                    doAddHandlermapping(clazz, singleton, baseUrl);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void doAddHandlermapping(Class<?> clazz, Object singleton, String baseUrl) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(BDRequestMapping.class)) {continue;}
            Handlermapping handlermapping = new Handlermapping();
            BDRequestMapping bdRequestMapping = method.getAnnotation(BDRequestMapping.class);
            String value = bdRequestMapping.value();
            baseUrl = "".equals(value) ? baseUrl : baseUrl + ("/" + value).replaceAll("//", "/");

            Map<String, Integer> paramSortMap = new HashMap<>(16);
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] parameterAnnotation = parameterAnnotations[i];
                for (int j = 0; j < parameterAnnotation.length; j++) {
                    Annotation annotation = parameterAnnotation[j];
                    if (annotation instanceof BDRequestParam) {
                        paramSortMap.put(((BDRequestParam) annotation).value(), i);
                    }
                }
                //处理HttpServletRequest和HttpServletResponse
                Class<?> parameterType = parameterTypes[i];
                if (parameterType == HttpServletRequest.class) {
                    paramSortMap.put(parameterType.getTypeName(), i);
                }
                if (parameterType == HttpServletResponse.class) {
                    paramSortMap.put(parameterType.getTypeName(), i);
                }
            }
            handlermapping
                    .setController(singleton)
                    .setMethod(method)
                    .setUrl(baseUrl)
                    .setParamSortMap(paramSortMap)
                    .setParameterTypes(parameterTypes);
            handlermappings.put(baseUrl, handlermapping);
        }
    }

    private void doDependancyInjection() {
        if (!ioc.isEmpty()) {
            try {
                for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                    Object singleton = entry.getValue();
                    Field[] fields = singleton.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        BDAutowired bdAutowired = field.getAnnotation(BDAutowired.class);
                        if (bdAutowired != null) {
                            String key = toLowercase(field.getType().getSimpleName());
                            if (!ioc.containsKey(key)) {
                                throw new RuntimeException("IOC容器中没有名为：" + key + "的实例");
                            }
                            field.set(singleton, ioc.get(key));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                        if (!f.getName().endsWith(".class")) {
                            continue;
                        }
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
