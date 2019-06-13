package com.zk.demo.servlet;

import com.zk.demo.annotation.ZKController;
import com.zk.demo.annotation.ZKRequestMapping;
import sun.security.util.Resources;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName:  <br/>
 * Function:  ADD FUNCTION. <br/>
 * Reason:  ADD REASON(可选). <br/>
 * date:   <br/>
 *
 * @author
 * @since JDK 1.6
 */
public class ZKDispatchServlet extends HttpServlet
{

    Properties prop = new Properties();

    List<String> classNames = new ArrayList<String>();

    Map<String, Object> IOCContext = new ConcurrentHashMap<>();

    Map<String, Method> handlerMapping = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        try
        {
            doDispatch(req, resp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            resp.getWriter().write("500 THIS REQUEST FAILED");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
        throws Exception
    {
        //获取请求路径
        String requestURI = req.getRequestURI();

        //获取请求参数
        String param = req.getParameter("name");
        //判断是否在mapping中
        if (!handlerMapping.containsKey(requestURI))
        {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }
        Method method = handlerMapping.get(requestURI);
        Class<?> clazz = method.getDeclaringClass();
        Object invoke = method.invoke(clazz.newInstance(), param);
        resp.getWriter().write(invoke+"");
    }

    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        //1.读取配置文件,拿到config里面配置的需要扫描的包名
        doLoadConfig(config.getInitParameter("classLoadConfig"));
        //解析配置文件,拿到所有的.class文件
        doScaner(prop.getProperty("base.scan.pack"));
        //Ioc,反射实例
        doInstance();
        //HandleMapping 匹配映射关系
        initMapping();
    }

    /**
     * 拿到了所有的className,需要拿到所有的HandlerMapping
     * Map<String,>方法级别   mappingUrl  Method对象
     */
    private void initMapping()
    {
        for (Map.Entry<String, Object> entry : IOCContext.entrySet())
        {
            String key = entry.getKey();
            Class clazz = entry.getValue().getClass();
            String baseMappingName = "";
            if (clazz.isAnnotationPresent(ZKRequestMapping.class))
            {
                ZKRequestMapping anno = (ZKRequestMapping)clazz.getAnnotation(ZKRequestMapping.class);
                baseMappingName = (anno.value());
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods)
            {
                if (method.isAnnotationPresent(ZKRequestMapping.class))
                {
                    ZKRequestMapping methodAnno = method.getAnnotation(ZKRequestMapping.class);
                    String mappingName = baseMappingName + methodAnno.value().replaceAll("/+", "/");
                    handlerMapping.put(mappingName, method);
                }
            }
        }
    }

    //初始化Ioc容器,并反射创建所有的bean实例
    private void doInstance()
    {
        if (classNames.isEmpty())
        {
            return;
        }
        for (String className : classNames)
        {
            try
            {
                Class<?> Clazz = Class.forName(className);
                //没有被controller注解修饰
                if (!Clazz.isAnnotationPresent(ZKController.class))
                {
                    continue;
                }

                //初始化实例
                Object o = Clazz.newInstance();
                String beanName = lowFirseCase(Clazz.getSimpleName());
                IOCContext.put(beanName, o);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * controller注解首字母都是小写的
     *
     * @param simpleName
     * @return
     */
    private String lowFirseCase(String simpleName)
    {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }

    private void doScaner(String scanpack)
    {
        URL url = ZKDispatchServlet.class.getClassLoader().getResource("/" + scanpack.replace(".", "/"));
        File file = new File(url.getFile());

        for (File innerFile : file.listFiles())
        {
            //文件夹需要递归调用,往下执行
            if (innerFile.isDirectory())
            {
                try
                {
                    doScaner(scanpack+"."+innerFile.getName());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            //不是.class文件,跳过
            String innerFileName = innerFile.getName();
            if (!innerFileName.endsWith(".class"))
            {
                continue;
            }
            innerFileName = scanpack+"."+innerFileName.replace(".class", "");
            classNames.add(innerFileName);
        }
    }

    private void doLoadConfig(String basePackName)

    {
        InputStream ins = this.getClass().getClassLoader().getResourceAsStream(basePackName);
        try
        {
            prop.load(ins);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != ins)
            {
                try
                {
                    ins.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}