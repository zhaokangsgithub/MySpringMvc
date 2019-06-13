package com.zk.demo.controller;

import com.zk.demo.annotation.ZKController;
import com.zk.demo.annotation.ZKRequestMapping;

@ZKController
@ZKRequestMapping("/myMvc/")
public class MyMvcController
{

	@ZKRequestMapping("hello")
	public String helloController(String name){
		System.out.println("name : "+name);
		return "name : "+name;
	}

}