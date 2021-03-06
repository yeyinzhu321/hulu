package com.xiaoleilu.huludemo.interceptor;

import com.xiaoleilu.hulu.ActionMethod;
import com.xiaoleilu.hulu.exception.ActionException;
import com.xiaoleilu.hulu.interceptor.Interceptor;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;

/**
 * 日志过滤器
 * @author loolly
 *
 */
public class Log2Interceptor implements Interceptor{
	private final static Log log = LogFactory.get();

	@Override
	public void invoke(ActionMethod actionMethod) throws ActionException {
		log.info("过滤器 [{}] 在执行Action方法前做的事情", this.getClass().getName());
		
		actionMethod.invoke();
		
		log.info("过滤器 [{}] 在执行Action方法后做的事情", this.getClass().getName());
	}

}
