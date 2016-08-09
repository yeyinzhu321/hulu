package com.xiaoleilu.hulu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.xiaoleilu.hulu.annotation.Route;
import com.xiaoleilu.hulu.exception.ActionException;
import com.xiaoleilu.hulu.interceptor.Interceptor;
import com.xiaoleilu.hulu.render.view.DefaultView;
import com.xiaoleilu.hulu.render.view.View;
import com.xiaoleilu.hutool.log.StaticLog;
import com.xiaoleilu.hutool.util.StrUtil;

/**
 * Action方法<br>
 * 单例存在于容器中
 * 
 * @author xiaoleilu
 */
public class ActionMethod {
	/** 过滤器执行位置记录器 */
	private static ThreadLocal<Integer> interceptorPosition = new ThreadLocal<Integer>();

	private Object action;						//Action对象
	private Method method;					//Action方法
	private String requestPath;				//请求路径
	private String httpMethod;				//HTTP方法（GET、POST等）
	private Interceptor[] interceptors;	//过滤器

	// -------------------------------------------------------------------- Constructor start
	public ActionMethod(Object action, Method method) {
		this.action = action;
		this.method = method;
		
		this.method.setAccessible(true); // 取消安全检查，加快invoke速度

		// 生成请求路径
		this.requestPath = genRequestPath();
	}

	public ActionMethod(Object action, Method method, Interceptor[] interceptors) {
		this(action, method);
		this.interceptors = interceptors;
	}

	// -------------------------------------------------------------------- Constructor end

	// ------------------------------------------------------------- Setters and Getters start
	/**
	 * 获得请求路径
	 * 
	 * @return 请求路径
	 */
	public String getRequestPath() {
		return requestPath;
	}

	/**
	 * 获得Action方法
	 * 
	 * @return Action方法
	 */
	protected Method getMethod() {
		return this.method;
	}
	
	/**
	 * 获得Http请求的方法，例如GET，POST等
	 * @return HTTP方法
	 */
	protected String getHttpMethod() {
		return this.httpMethod;
	}

	// ------------------------------------------------------------- Setters and Getters end

	/**
	 * 执行Action方法<br>
	 * 同时会执行过滤器方法<br>
	 * 此方法为递归调用，每次递归调用此方法，都会判断执行到了第几个拦截器，从而执行拦截器。<br>
	 * 当拦截器数量执行完毕后，执行本体方法
	 * 
	 * @throws ActionException
	 */
	public void invoke() throws ActionException {
		Integer position = interceptorPosition.get();
		if (position == null) {
			position = 0;
		}

		if (interceptors != null && position < interceptors.length) {
			//执行过滤器，递归调用本方法
			interceptorPosition.set(position + 1);
			interceptors[position].invoke(this);
		}else {
			//过滤器执行完毕，执行本体方法
			invokeActionMethod();
			// 执行了Action本体方法，说明过滤器使用完毕，清理游标防止重复执行
			resetInterceptorPosition();
		}
	}
	
	/**
	 * 执行本体方法
	 * @throws ActionException 
	 */
	protected void invokeActionMethod() throws ActionException {
		Object returnValue;
		try {
			//TODO 支持参数注入
			returnValue = this.method.invoke(this.action);
		} catch(InvocationTargetException te) {
			throw new ActionException(te.getCause());
		} catch (Exception e) {
			throw new ActionException("Invoke action method error!", e);
		}
		
		//对于带有返回值的Action方法，执行Render
		if(null != returnValue) {
			if(false == (returnValue instanceof View)) {
				//将未识别响应对象包装为View
				returnValue = DefaultView.wrap(returnValue);
			}
			((View) returnValue).render();
		}
	}

	/**
	 * 重置过滤器执行顺序游标<br>
	 * 游标记录了执行到了第几个过滤器
	 */
	protected void resetInterceptorPosition() {
		interceptorPosition.remove();
	}
	
	/**
	 * 指定用户请求的HTTP方法是否和定义的方法匹配<br>
	 * 用户只有在Route注解中定义方法后才会匹配有效性
	 * @return 是否匹配
	 */
	protected boolean isHttpMethodMatch() {
		if(StrUtil.isNotBlank(httpMethod) && httpMethod.equalsIgnoreCase(Request.getServletRequest().getMethod()) == false) {
			if(HuluSetting.isDevMode) {
				StaticLog.warn("Request [{}] method [{}] is not match [{}]", requestPath, Request.getServletRequest().getMethod(), httpMethod);
			}
			return false;
		}
		return true;
	}

	// ------------------------------------------------------------- Private method start
	/**
	 * 生成请求路径<br>
	 * @return 请求路径
	 */
	private String genRequestPath() {
		//Action路径
		String actionPath = getPath(this.action);
		
		// 根据Annotation自定义请求路径
		String methodPath = getPath(this.method);

		//提取HTTP方法名（如果路径是类似于get:/test/testMethod）
		if(methodPath != null && methodPath.contains(":")) {
			final List<String> methodAndPath = StrUtil.split(methodPath, ':', 2);
			this.httpMethod = methodAndPath.get(0).trim().toUpperCase();
			methodPath = methodAndPath.get(1).trim();
		}
		
		return StrUtil.format("{}{}", fixPath(actionPath), fixPath(methodPath));
	}
	
	/**
	 * 修正请求路径<br>
	 * 1、去除空白符
	 * 2、去除尾部斜杠
	 * 3、补全开头的斜杠
	 * @param path 原请求路径
	 * @return 修正后的请求路径
	 */
	private static String fixPath(String path) {
		path = StrUtil.cleanBlank(path);							//去除空白符
		path = StrUtil.removeSuffix(path, StrUtil.SLASH); // 去除尾部斜杠
		if (path.startsWith(StrUtil.SLASH) == false) {
			path = StrUtil.SLASH + path;							//在路径前补全“/”
		}
		
		return path;
	}
	
	/**
	 * 获得Route注解的自定义请求路径<br>
	 * @param obj Action对象或者Method对象
	 * @return 处理后的请求路径，无定义为null
	 */
	private static String getPath(Object obj) {
		if(null == obj){
			return null;
		}
		
		String routePath;
		Route routeAnnotation;
		if(obj instanceof Method){
			Method method = (Method)obj;
			routeAnnotation = method.getAnnotation(Route.class);
			if(null != routeAnnotation){
				routePath = routeAnnotation.value();
			}else{
				routePath = method.getName();
			}
		}else{
			routeAnnotation = obj.getClass().getAnnotation(Route.class);
			if(null != routeAnnotation){
				routePath = routeAnnotation.value();
			}else{
				routePath = StrUtil.lowerFirst(StrUtil.removeSuffix(obj.getClass().getSimpleName(), HuluSetting.actionSuffix));
			}
		}
		return routePath;
	}
	// ------------------------------------------------------------- Private method end
}
