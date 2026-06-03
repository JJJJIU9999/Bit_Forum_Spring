package com.bitforum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bitforum.mapper")
public class BitForumSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(BitForumSpringApplication.class, args);
	}

}
