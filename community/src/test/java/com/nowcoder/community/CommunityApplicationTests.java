package com.nowcoder.community;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CommunityApplicationTests implements ApplicationContextAware {

    private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
	}

	@Test
	public void testApplicationContext(){
		System.out.println(applicationContext);
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class); //getBean 从容器中获取bean实例 //通过优先级来获取
		System.out.println(alphaDao.select());

		alphaDao = applicationContext.getBean("AlphaHibernate", AlphaDao.class); //从容器中获取bean实例，通过bean容器的名字来获取
		//将得到的object "AlphaHibernate"转换为 AlphaDao.class
		System.out.println(alphaDao.select());
	}

	@Test
	public void testBeanManagement(){ //测试bean的管理方式
		AlphaService alphaService = applicationContext.getBean(AlphaService.class); //获取bean
		System.out.println(alphaService);//打印出bean的实例
		//alphaService = applicationContext.getBean(AlphaService.class); //获取bean
		//System.out.println(alphaService);//打印出bean的实例
	}

	@Test
	public void testBeanConfig() { //测试bean
		SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
		System.out.println(simpleDateFormat.format((new Date())));
	}

	@Autowired
	private AlphaDao alphaDao; //给当前bean注入AlphaDao

	@Autowired
	private AlphaService alphaService; //给当前bean注入AlphaService

	@Autowired
	private SimpleDateFormat simpleDateFormat; //给当前bean注入SimpleDateFormat

	@Test
	public void testDI(){ //测试依赖注入
		System.out.println(alphaDao);
		System.out.println(alphaService);
		System.out.println(simpleDateFormat);
	}
}
