package com.xiaoleilu.hulu.render.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.xiaoleilu.hulu.render.ErrorRender;

/**
 * 500错误视图
 * @author Looly
 *
 */
public class Error500View implements View{
	
	private Throwable e;
	
	//---------------------------------------------------------- Constructor start
	public Error500View() {
	}

	public Error500View(Throwable e) {
		this.e = e;
	}
	//---------------------------------------------------------- Constructor end
	
	//---------------------------------------------------------- Getters and Setters start
	public Throwable getE() {
		return e;
	}
	public void setE(Throwable e) {
		this.e = e;
	}
	//---------------------------------------------------------- Getters and Setters end

	@Override
	public void render() {
		ErrorRender.render500(e);
	}
	
	@Override
	public String toString() {
		final StringWriter writer = new StringWriter();
		// 把错误堆栈储存到流中
		this.e.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
}
