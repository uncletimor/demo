package bos_fore;

import org.junit.Test;

import redis.clients.jedis.Jedis;

public class Demo {
	@Test
	public void test(){
		Jedis jedis = new Jedis("127.0.0.1",6379);
		jedis.set("test","5545446465464654646564");
	}
	
	@Test
	public void test2(){
		Jedis jedis = new Jedis("127.0.0.1",6379);
		jedis.set("test","5545446465464654646564");
		System.out.println(jedis.get("test"));
	}
	
}
