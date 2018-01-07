   package cn.itcast.fore.action;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Controller;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.opensymphony.xwork2.ActionSupport;

import cn.itcast.bos.utils.MailUtils;
import cn.itcast.bos.utils.Md5Util;
import cn.itcast.bos.utils.SendMessageUtils;
import cn.itcast.crm.service.Customer;
import cn.itcast.crm.service.CustomerService;


@Controller
@Namespace("/")
@ParentPackage("struts-default")
@Scope("prototype")
@Results({@Result(name="signup_success",type="redirect",location="/signup-success.html"),
	@Result(name="signup",location="/signup.html")})
public class CustomerAction extends BaseAction<Customer>{
	
	//接收页面提供的验证码
	private String checkcode;
	public void setCheckcode(String checkcode) {
		this.checkcode = checkcode;
	}
	private String checkCode2;
	//接收客户邮件中提交激活码
		private String activecode;
		public void setActiveCode(String activecode) {
			this.activecode = activecode;
		}
	//注入service
	@Autowired
	private CustomerService crmProxy;
	//获取redis的模板
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Autowired  //注入发短信模板
	private JmsTemplate jmsTemplate;
	
	/**
	 * @author Darry
	 *调用平台发送短信
	 */		
	@Action("customerAction_sendCheckcode")
	public String sendCheckcode() throws Exception {
		//生成4位验证码
		
		//获取httpresponse
		HttpServletResponse res = ServletActionContext.getResponse();
		res.setContentType("text:html;charset=utf-8");
		//获取返回验证码
		 checkCode2 = SendMessageUtils.sendCheckCode(model.getTelephone());
		if(checkCode2.length()==4){
			//存进session
			ServletActionContext.getRequest().getSession().setAttribute(model.getTelephone(), checkCode2);
			ServletActionContext.getResponse().getWriter().write("1");
			return NONE;
		}else{
			ServletActionContext.getResponse().getWriter().write("0");
			return NONE;
		}
	}
	/**
	 * 注册用户
	 */
	@Action("customerAction_saveCustomer")
	public String saveCustomer() throws Exception {
		//判断验证码是否正确 调用工具类判断是否为空从session中获取真的验证码
		String  attribute = (String) ServletActionContext.getRequest().getSession().getAttribute(model.getTelephone());
		if(StringUtils.isNotBlank(checkcode)){
			//判断是否过期
			if(checkcode.equals(attribute)){
				//密码是明文通过MD5对其进行加密
				String newpwd = Md5Util.encode(model.getPassword());
				//用户保存新的密文
				model.setPassword(newpwd);
				
				
				crmProxy.save(model);
				//注册成功 发送邮件给客户
				//生成激活码
				String activecode = UUID.randomUUID().toString();
				//生成通知体
				String content = "欢迎您注册速运快递，为了提供更好的服务，请您在24小时内激活账户!!</br>"
						+ "<a href='"+MailUtils.activeUrl+"?telephone="+model.getTelephone()+"&activeCode="+activecode+"'>点击激活账户</a>";
				///调用工具类发送邮件
				MailUtils.sendMail("欢迎注册速运快递员", content, model.getEmail());
				//SendMessageUtils.sendWorkBill(model.getTelephone(),"【传智】恭喜注册成功，请您在24小时内登陆邮箱进行账户激活！");
				//将邮箱中激活码存储到redis内存数据库中:24小时内有效
				//redisTemplate.opsForValue().set(model.getTelephone(), activecode, 24, TimeUnit.HOURS);
				//通过jmsTempalate对象向队列中写入发短信消息
				jmsTemplate.send("sms-msg",new MessageCreator() {
					
					@Override
					public Message createMessage(Session session) throws JMSException {
						String msg = "【传智】恭喜注册成功，请您在24小时内登陆邮箱进行账户激活！";
						MapMessage mapMessage = session.createMapMessage();
						mapMessage.setString("tel", model.getTelephone());
						mapMessage.setString("msg", msg);
						return mapMessage;
					}
				});
				//将session中验证码移除
				ServletActionContext.getRequest().getSession().removeAttribute(model.getTelephone());
				return "signup_success";
			}else{
				this.addActionError("验证码输入错误!");
				return "signup";
			}
		}else{
			this.addActionError("验证码过期!");
			return "signup";
		}
		
			
	}
	/**
	 * 激活用户
	 * @throws Exception 
	 * 
	 */
	@Action("customerAction_activeMail")
	public String activeMail() throws Exception{
		//接收激活邮箱传来的参数 判断是不是空值
		if(StringUtils.isNotBlank(model.getTelephone())){
			//通过手机号查询客户
			Customer customer = crmProxy.findByTelephone(model.getTelephone());
			//判断客户是不是存在
			if(customer!=null){//客户存在 且已经激活
				if(customer.getType()==null){//客户存在,但是尚未激活
					//判断缓存中是否存在激活码
					String realActiveCode = redisTemplate.opsForValue().get(model.getTelephone());
						if(StringUtils.isNotBlank(realActiveCode)){//激活码存在  可以激活
							//判断用户激活码是否正确
							if(activecode.equals(realActiveCode)){//激活码正确  激活账户
								crmProxy.activeAccount(customer.getId());
								//删除缓存中数据
								redisTemplate.delete(customer.getTelephone());
								//给出友好提示
								ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
								ServletActionContext.getResponse().getWriter().write("恭喜注册成功！请完善个人信息！");
								return NONE;
							}else{//验证码错误
								ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
								ServletActionContext.getResponse().getWriter().write("验证码错误！");
								return NONE;
							}
						}else{//验证码不存在
							ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
							ServletActionContext.getResponse().getWriter().write("验证码已失效！");
							return NONE;
						}
				}else{//客户存在  已激活
					ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
					ServletActionContext.getResponse().getWriter().write("账户已激活,无需再次激活！");
					return NONE;
				}
			}else{
				//客户存在  已激活
				ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
				ServletActionContext.getResponse().getWriter().write("激活链接有误.请联系管理员！");
				return NONE;
			}
		}else{
			//客户存在  已激活
			ServletActionContext.getResponse().setContentType("text/html;charset=utf-8");
			ServletActionContext.getResponse().getWriter().write("激活链接有误！");
			return NONE;
		}
	}
	/**
	 * 用户登录
	 */
	@Action("customerAction_loginByUsername")
	public String loginByUsername() throws Exception {
		//从session里获取验证码
		String  realcode = (String) ServletActionContext.getRequest().getSession().getAttribute("key");
		if(checkcode.equals(realcode)){//判断用户输入的验证码是否正确
			//通过用户名和密码进行查找    将用户输入的密码进行加密处理
			Customer c = crmProxy.loginByUsername(model.getUsername(), Md5Util.encode(model.getPassword()));
			if(c!=null){//判断用户是否存在  
				//用户存在  保存到session中
				ServletActionContext.getRequest().getSession().setAttribute("custoemr", c);
				ServletActionContext.getResponse().getWriter().write("ok");
				return NONE;
			}else{
				//用户不存在  返回1
				ServletActionContext.getResponse().getWriter().write("no");
				return NONE;
			}
		
		}else{
			//验证码错误
			ServletActionContext.getResponse().getWriter().write("wrong");
			return NONE;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}












