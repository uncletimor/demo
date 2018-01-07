package cn.itcast.fore.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.itcast.bos.domain.base.Area;
import cn.itcast.bos.domain.base.Order;
import cn.itcast.bos.service.take_delivery.OrderService;
import cn.itcast.crm.service.Customer;
@Controller
@Namespace("/")
@ParentPackage("struts-default")
@Scope("prototype")
@Results({@Result(name="order_success",type="redirect",location="/order_success.html")})
public class OrderAction extends BaseAction<Order> {
		@Autowired
		private  OrderService orderProxy;
	
		
		//用属性驱动接收表单地址信息 
		private String sendAreaInfo;
		private String recAreaInfo;
		public void setSendAreaInfo(String sendAreaInfo) {
			this.sendAreaInfo = sendAreaInfo;
		}
		public void setRecAreaInfo(String recAreaInfo) {
			this.recAreaInfo = recAreaInfo;
		}
		
		
		/**
		 * 订单保存  调用代理保存订单
		 * @return
		 * @throws Exception
		 */
		@Action("orderAction_save")
		public String save() throws Exception {
			if(StringUtils.isNotBlank(sendAreaInfo)){
				String[] strings = sendAreaInfo.split("/");
				Area area = new Area(strings[0],strings[1],strings[2]);
				model.setSendArea(area);
			}
			if(StringUtils.isNotBlank(recAreaInfo)){
				String[] strings = recAreaInfo.split("/");
				Area recArea = new Area(strings[0],strings[1],strings[2]);
				model.setRecArea(recArea);
			}
			//绑定客户
			Customer customer = (Customer)ServletActionContext.getRequest().getSession().getAttribute("custoemr");
			if(customer!=null){
				model.setCustomer_id(customer.getId());
			}
			orderProxy.save(model);
		return "order_success";
		}



		
}
    