package cn.itcast.fore.action;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.springframework.data.domain.Page;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;
/*
 * 公共action
 * **将模型驱动getModel方法统一提取
 * **将一些通过代码统一提取
 */

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
public class BaseAction<T> extends ActionSupport implements ModelDriven<T> {
	
	//泛型相关术语
		/**
		 * BaseAction<Standard>  参数化类型
		 * <>中内容  实际类型参数  ----- 目的
		 * */
	protected T model;
	public T getModel() {
		// TODO Auto-generated method stub
		return model;
	}
	/**
	 * 子类对象创建父类无参构造执行
	 * 在无参构造中将实际类型参数获取到
	 * @throws Exception 
	 * @throws InstantiationException 
	 */
	public BaseAction(){
		try {
			//获取当前运行的子类class
		Class c1 = this.getClass();//子类的
		//获取参数或类型 
		//返回值表示		
		Type type = c1.getGenericSuperclass();
		//向下转型：转成子接口
		ParameterizedType pt = (ParameterizedType) type;
		// 3、获取实际类型参数
		//Type[] getActualTypeArguments() 
		//返回表示此类型实际类型参数的 Type 对象的数组。 
		Type[] types = pt.getActualTypeArguments();
		Class c2 = (Class)types[0];
		//将实际类型参数 实例化
			model = (T) c2.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//设置公用分页参数
	// 设置数据分叶查询要用的数据和属性 接收提交的参数
	protected int page;
	protected int rows;
			
			public void setPage(int page) {
				this.page = page;
			}
			public void setRows(int rows) {
				this.rows = rows;
			}
			
			/**
			  * @Description: 将page结果转为分页查询json
			  * 将java Map对象转为json字符串
			  * @param page ：分页查询结果
			  * @param excludes :转json排除属性
			 */
			public void ToJson(Page<T> page,String[] excludes){
				try {
				Map<String,Object> map = new HashMap<>();
				map.put("rows",page.getContent());
				map.put("total",page.getTotalElements());
				//将数据转成字符串 排除集合属性
				JsonConfig js = new JsonConfig();
				js.setExcludes(excludes);
				String json = JSONObject.fromObject(map, js).toString();
				HttpServletResponse response = ServletActionContext.getResponse();
				response.setContentType("text/html;charset=utf-8");
				response.getWriter().write(json);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			/**
			  * @Description: 将page结果转为分页查询json
			  * 将java list 数组 对象转为json字符串
			  * @param page ：分页查询结果
			  * @param excludes :转json排除属性
			 */
			public void ToJson(List list,String[] excludes){
				try {
				JsonConfig jc = new JsonConfig();
				jc.setExcludes(excludes);
				String json = JSONArray.fromObject(list, jc).toString();
				HttpServletResponse response = ServletActionContext.getResponse();
				response.setContentType("text/html;charset=utf-8");
				response.getWriter().write(json);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}
